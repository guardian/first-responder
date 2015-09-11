import play.api.{ Application, ApplicationLoader }
import play.api.ApplicationLoader.Context

class AppLoader extends ApplicationLoader {
  override def load(context: Context): Application = {
    val components = new AppComponents(context)

    println(
      """
        |    WELCOME TO FIRST RESPONDER
        |           .--._.--.
        |          ( O     O )
        |          /   . .   \
        |         .`._______.'.
        |        /(           )\
        |      _/  \  \   /  /  \_
        |   .~   `  \  \ /  /  '   ~.
        |  {    -.   \  V  /   .-    }
        |_ _`.    \  |  |  |  /    .'_ _
        |>_       _} |  |  | {_       _<
        | /. - ~ ,_-'  .^.  `-_, ~ - .\
        |         '-'|/   \|`-`
      """.stripMargin)

    components.application
  }
}
