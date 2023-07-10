package mlb

import zio._
import zio.jdbc._
import zio.http._
import zio.json.EncoderOps
import Database.*

object Endpoints {
  val endpoints: App[ZConnectionPool] =
    Http
      .collectZIO[Request] {

        case Method.GET -> Root / "init" =>
          (for {
            _ <- dropTables
            _ <- initializeDatabaseLogic
            res = Response.text("Database initialized successfully")
          } yield res) catchAll { _ =>
            ZIO.succeed(Response.text("Database initialization failed").withStatus(Status.InternalServerError))
          }

        case Method.GET -> Root / "games" / "count" =>
          (for {
            count: Option[String] <- countGames
            res: Response = count match
              case Some(c) => Response.text(s"${c} games")
              case None    => Response.text("No game in historical data").withStatus(Status.NoContent)
          } yield res) catchAll { _ =>
            ZIO.succeed(Response.text("No games found").withStatus(Status.NotFound))
          }
        
        case Method.GET -> Root / "games" / "new" => 
          (for {
            teams <- selectGames
            res = teams match {
              case teams if teams.nonEmpty =>
                val teamsJson = teams.toJson
                Response.json(s"""{"teams": ${teamsJson}}""")
              case _ =>
                Response.text("No teams found")
            }
          } yield res) catchAll { _ =>
            ZIO.succeed(Response.text("No teams found").withStatus(Status.NotFound))
          }
      }
      .withDefaultErrorResponse
}
