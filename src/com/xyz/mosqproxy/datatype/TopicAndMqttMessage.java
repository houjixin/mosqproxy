package com.xyz.mosqproxy.datatype;

import org.eclipse.paho.client.mqttv3.MqttMessage;

public class TopicAndMqttMessage
{
	private String topic;
	private MqttMessage mqttMessage;
	
	public String getTopic()
	{
		return topic;
	}

	public void setTopic(String topic)
	{
		this.topic = topic;
	}

	public MqttMessage getMqttMessage()
	{
		return mqttMessage;
	}

	public void setMqttMessage(MqttMessage mqttMessage)
	{
		this.mqttMessage = mqttMessage;
	}

	public TopicAndMqttMessage(String topic, MqttMessage msg)
	{
		this.topic = topic;
		this.mqttMessage = msg;
	}
	
	public String toString()
	{
		return String.format("[%s]: %s", topic, mqttMessage.toString());
	}
}
