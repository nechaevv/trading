package ru.osfb.trading.notification

import java.net.URLEncoder

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.typesafe.scalalogging.LazyLogging
import ru.osfb.webapi.core.{ActorMaterializerComponent, ActorSystemComponent, ConfigurationComponent, ExecutionContextComponent}
import ru.osfb.webapi.utils.FutureUtils._
import scala.concurrent.Future

/**
  * Created by sgl on 10.04.16.
  */
trait PushAllNotificationServiceComponentImpl extends NotificationServiceComponent {
  this: ActorSystemComponent
    with ActorMaterializerComponent
    with ExecutionContextComponent
    with ConfigurationComponent =>

  class NotificationServiceImpl extends NotificationService with LazyLogging {
    private lazy val subscriptionId = configuration.getString("notification.pushall.subscription-id")
    private lazy val subscriptionKey = configuration.getString("notification.pushall.subscription-key")

    override def notify(text: String): Future[Unit] = Http().singleRequest(HttpRequest(
      uri = s"https://pushall.ru/api.php?type=self&id=$subscriptionId&key=$subscriptionKey&title=TradeBot&text=${URLEncoder.encode(text,"UTF-8")}"
    )).flatMap(r => Unmarshal(r.entity).to[String]).map(result => {
      logger.trace(s"Sent notification: $text, result: $result")
    }).withErrorLog(logger)
  }

}
