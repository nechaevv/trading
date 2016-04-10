package ru.osfb.trading.notification

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.scaladsl.Sink
import ru.osfb.webapi.core.{ActorMaterializerComponent, ActorSystemComponent, ConfigurationComponent, ExecutionContextComponent}

import scala.concurrent.Future

/**
  * Created by sgl on 10.04.16.
  */
trait PushAllNotificationServiceComponentImpl extends NotificationServiceComponent {
  this: ActorSystemComponent
    with ActorMaterializerComponent
    with ExecutionContextComponent
    with ConfigurationComponent =>

  class NotificationServiceImpl extends NotificationService {
    private lazy val subscriptionId = configuration.getString("notification.pushall.subscription-id")
    private lazy val subscriptionKey = configuration.getString("notification.pushall.subscription-key")

    override def notify(text: String): Future[Unit] = Http().singleRequest(HttpRequest(
      uri = s"https://pushall.ru/api.php?type=self&id=$subscriptionId&key=$subscriptionKey&title=TrendBot&text=$text"
    )).map(_.entity.dataBytes.runWith(Sink.ignore))
  }

}
