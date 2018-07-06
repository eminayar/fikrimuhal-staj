final case class ResumeException( private val message: String = "", private val cause: Throwable = None.orNull ) extends Exception
final case class RestartException( private val message: String = "", private val cause: Throwable = None.orNull ) extends Exception
final case class StopException( private val message: String = "", private val cause: Throwable = None.orNull ) extends Exception
final case class EscalateException( private val message: String = "", private val cause: Throwable = None.orNull ) extends Exception