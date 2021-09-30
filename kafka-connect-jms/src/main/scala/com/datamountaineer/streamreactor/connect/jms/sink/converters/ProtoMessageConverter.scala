package com.datamountaineer.streamreactor.connect.jms.sink.converters

import com.datamountaineer.streamreactor.common.schemas.ConverterUtil
import com.datamountaineer.streamreactor.connect.jms.config.JMSSetting
import org.apache.kafka.connect.errors.ConnectException
import org.apache.kafka.connect.sink.SinkRecord

import java.io.IOException
import java.util
import javax.jms.{BytesMessage, JMSException, Message, Session}

class ProtoMessageConverter extends JMSMessageConverter with ConverterUtil {

  private val dynamicConverter = new ProtoDynamicConverter
  private val storedAsConverter = new ProtoStoredAsConverter

  override def initialize(map: util.Map[String, String]): Unit = {
    dynamicConverter.initialize(map)
    storedAsConverter.initialize(map)
  }

  override def convert(record: SinkRecord, session: Session, setting: JMSSetting): Tuple2[String, BytesMessage] = {
    val protoConverter = if (setting.storedAs == null) dynamicConverter
    else storedAsConverter
    try {
      val bytes = protoConverter.convert(record, setting)
      val message = session.createBytesMessage
      message.writeBytes(bytes)
      val convertedMessage = new Tuple2[String, BytesMessage](setting.source, message)
      convertedMessage
    } catch {
      case e@(_: IOException | _: JMSException) =>
        throw new ConnectException(e.getMessage, e)
    }
  }
}
