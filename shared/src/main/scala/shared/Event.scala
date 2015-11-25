package shared

trait Event {
  def name: String
  def timestamp: Long
}

case class OverflowEvent(dropped: Long, timestamp: Long) extends Event {
  override def name: String = "overflow-event"
}

case class Signal(value: Double, timestamp: Long) extends Event {
  override def name: String = "signal-event"
}