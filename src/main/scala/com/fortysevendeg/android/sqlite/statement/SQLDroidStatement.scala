package com.fortysevendeg.android.sqlite.statement

import java.sql._

import com.fortysevendeg.android.sqlite.logging.{AndroidLogWrapper, LogWrapper}
import com.fortysevendeg.android.sqlite.resultset.SQLDroidResultSet
import com.fortysevendeg.android.sqlite.{SQLDroidConnection, SQLDroidDatabase, WrapperNotSupported, _}

import scala.collection.mutable

class SQLDroidStatement(
  sqlDroidConnection: SQLDroidConnection,
  columnGenerated: Option[String] = None,
  logWrapper: LogWrapper = new AndroidLogWrapper()) extends Statement with WrapperNotSupported {

  val selectRegex = "(?m)(?s)\\s*(SELECT|PRAGMA|EXPLAIN QUERY PLAN).*".r

  val connectionClosedErrorMessage = "Connection is closed"

  val statementClosedErrorMessage = "Statement is closed"

  val maxNegativeErrorMessage = "Max rows must be zero or positive"

  val selectLastRowId = "SELECT last_insert_rowid()"

  private[this] val batchList = new mutable.MutableList[String]

  private[this] var connection: Option[SQLDroidConnection] = Option(sqlDroidConnection)

  private[this] var maxRows: Option[Int] = None

  private[this] var resultSet: Option[ResultSet] = None

  private[this] var updateCount: Option[Int] = None

  def getBatchList: Seq[String] = batchList.toList

  override def addBatch(sql: String): Unit = batchList += sql

  override def clearBatch(): Unit = batchList.clear()

  override def close(): Unit = {
    connection = None
    closeResultSet()
  }

  override def isClosed: Boolean = connection.isEmpty

  override def execute(sql: String): Boolean = withOpenConnection { db =>
    resultSet = selectRegex.pattern.matcher(sql).matches() match {
      case true =>
        updateCount = None
        val limitedSql = maxRows map (m => s"$sql LIMIT $m") getOrElse sql
        Some(new SQLDroidResultSet(db.rawQuery(limitedSql)))
      case false =>
        db.execSQL(sql)
        updateCount = Option(db.changedRowCount())
        None
    }
    resultSet.isDefined
  }

  override def executeBatch(): scala.Array[Int] = withOpenConnection { db =>
    val updateArray = batchList map { sql =>
      db.execSQL(sql)
      db.changedRowCount()
    }
    updateCount = updateArray.reduceOption { (elem1, elem2) =>
      (elem1, elem2) match {
        case (a, b) if a > 0 && b > 0 => a + b
        case (a, b) if a > 0 => a
        case (a, b) if b > 0 => b
        case _ => 0
      }
    }
    updateArray.toArray
  }

  override def executeQuery(sql: String): ResultSet =
    withOpenConnection(newQueryResultSet(_, sql))

  override def executeUpdate(sql: String): Int = withOpenConnection { db =>
    db.execSQL(sql)
    val count = db.changedRowCount()
    updateCount = Some(count)
    count
  }

  override def getConnection: Connection =
    connection match {
      case Some(c) => c
      case _ => throw new SQLException(connectionClosedErrorMessage)
    }

  override def getGeneratedKeys: ResultSet =
    withOpenConnection(newQueryResultSet(_, s"$selectLastRowId ${columnGenerated getOrElse ""}"))

  override def getMaxRows: Int = maxRows getOrElse 0

  override def setMaxRows(max: Int): Unit =
    max match {
      case _ if isClosed => throw new SQLException(statementClosedErrorMessage)
      case n if n < 0 => throw new SQLException(s"$maxNegativeErrorMessage Got $n")
      case n if n == 0 => maxRows = None
      case _ => maxRows = Some(max)
    }

  override def getMoreResults: Boolean = getMoreResults(Statement.CLOSE_CURRENT_RESULT)

  override def getMoreResults(current: Int): Boolean = withOpenConnection { _ =>
    if (current == Statement.CLOSE_CURRENT_RESULT) closeResultSet()
    false
  }

  override def getUpdateCount: Int = {
    val count = updateCount getOrElse -1
    updateCount = None
    count
  }

  override def execute(sql: String, autoGeneratedKeys: Int): Boolean = logWrapper.notImplemented(false)

  override def execute(sql: String, columnIndexes: scala.Array[Int]): Boolean = logWrapper.notImplemented(false)

  override def execute(sql: String, columnNames: scala.Array[String]): Boolean = logWrapper.notImplemented(false)

  override def executeUpdate(sql: String, autoGeneratedKeys: Int): Int = logWrapper.notImplemented(0)

  override def executeUpdate(sql: String, columnIndexes: scala.Array[Int]): Int = logWrapper.notImplemented(0)

  override def executeUpdate(sql: String, columnNames: scala.Array[String]): Int = logWrapper.notImplemented(0)

  override def cancel(): Unit = logWrapper.notImplemented(Unit)

  override def clearWarnings(): Unit = logWrapper.notImplemented(Unit)

  override def getFetchDirection: Int = logWrapper.notImplemented(0)

  override def setFetchDirection(direction: Int): Unit = logWrapper.notImplemented(Unit)

  override def getFetchSize: Int = logWrapper.notImplemented(0)

  override def setFetchSize(rows: Int): Unit = logWrapper.notImplemented(Unit)

  override def getMaxFieldSize: Int = logWrapper.notImplemented(0)

  override def setMaxFieldSize(max: Int): Unit = logWrapper.notImplemented(Unit)

  override def getQueryTimeout: Int = logWrapper.notImplemented(0)

  override def setQueryTimeout(seconds: Int): Unit = logWrapper.notImplemented(Unit)

  override def getResultSet: ResultSet = resultSet getOrElse javaNull

  override def getResultSetConcurrency: Int = logWrapper.notImplemented(0)

  override def getResultSetHoldability: Int = logWrapper.notImplemented(0)

  override def getResultSetType: Int = logWrapper.notImplemented(0)

  override def getWarnings: SQLWarning = logWrapper.notImplemented(javaNull)

  override def setCursorName(name: String): Unit = logWrapper.notImplemented(Unit)

  override def setEscapeProcessing(enable: Boolean): Unit = logWrapper.notImplemented(Unit)

  override def setPoolable(poolable: Boolean): Unit = logWrapper.notImplemented(Unit)

  override def isPoolable: Boolean = logWrapper.notImplemented(false)

  override def isCloseOnCompletion: Boolean = logWrapper.notImplemented(false)

  override def closeOnCompletion(): Unit = logWrapper.notImplemented(Unit)

  private[this] def closeResultSet() = {
    resultSet foreach { rs =>
      if (!rs.isClosed) rs.close()
    }
    resultSet = None
  }

  private[this] def withOpenConnection[T](f: (SQLDroidDatabase) => T) =
    connection match {
      case Some(c) =>
        closeResultSet()
        c.withOpenDatabase[T](f)
      case _ =>
        throw new SQLException(connectionClosedErrorMessage)
    }

  private[this] def newQueryResultSet(db: SQLDroidDatabase, sql: String): ResultSet = {
    val rs = new SQLDroidResultSet(db.rawQuery(sql))
    resultSet = Some(rs)
    rs
  }
}
