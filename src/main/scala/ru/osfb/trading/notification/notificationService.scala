package ru.osfb.trading.notification

import scala.concurrent.Future

/**
  * Created by sgl on 10.04.16.
  */
trait NotificationService {
  def notify(text: String): Future[Unit]
}

trait NotificationServiceComponent {
  def notificationService: NotificationService
}
