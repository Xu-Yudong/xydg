package Impl


import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import io.circe.Json
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits.*
import Common.DBAPI._
import Common.API.{PlanContext, Planner}
import cats.effect.IO
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Common.ServiceUtils.schemaName


case class DeleteFriendsByUserIDMessagePlanner(
                                                userID: String,
                                                override val planContext: PlanContext
                                              ) extends Planner[String] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[String] = {
    for {
      // Step 1: Query all friend relations associated with the given userID
      _ <- IO(logger.info(s"开始从FriendRelationTable中查找${userID}相关的好友关系记录"))
      friendRelations <- findFriendRelations

      _ <- IO(logger.info(s"找到好友关系记录数：${friendRelations.size}，执行删除操作"))

      // Step 2: Delete all friend relations associated with the userID
      deleteResult <- deleteFriendRelations()

      _ <- IO(logger.info(s"删除操作完成，结果：${deleteResult}"))

    } yield "Operation(s) done successfully"
  }

  private def findFriendRelations(using PlanContext): IO[List[Json]] = {
    val sql =
      s"""
         SELECT * FROM ${schemaName}.friend_relation_table
         WHERE user_id = ?;
       """
    logger.info(s"SQL语句为：${sql}")
    readDBRows(sql, List(SqlParameter("String", userID)))
  }

  private def deleteFriendRelations()(using PlanContext): IO[String] = {
    val sql =
      s"""
         DELETE FROM ${schemaName}.friend_relation_table
         WHERE user_id = ?;
       """
    logger.info(s"SQL语句为：${sql}")
    writeDB(sql, List(SqlParameter("String", userID)))
  }
}