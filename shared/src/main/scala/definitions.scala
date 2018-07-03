final case class User(username: String, password: String)
case object GetUsers
final case class CreateUser(user: User)
final case class Login(user: User)
final case class Logout(token: String)
final case class isValidToken(token: String)
case object ShutDown
final case class Quote(id: Int, body: String)
final case class QuoteActionPerformed( description: String )
final case class CreateQuote(body: String)
case object GetQuotes
final case class EraseQuote(id: Int)
final case class ChangeQuote(id: Int, body: String)
case object FeaturedQuote
