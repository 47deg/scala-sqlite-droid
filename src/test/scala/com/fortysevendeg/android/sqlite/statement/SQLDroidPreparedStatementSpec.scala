package com.fortysevendeg.android.sqlite.statement

import java.io.{Reader, StringReader, ByteArrayInputStream, InputStream}
import java.sql._
import java.util.Calendar

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.fortysevendeg.android.sqlite.logging.LogWrapper
import com.fortysevendeg.android.sqlite.resultset.SQLDroidResultSet
import com.fortysevendeg.android.sqlite.{SQLDroidConnection, TestLogWrapper, _}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

trait SQLDroidPreparedStatementSpecification
  extends Specification
  with Mockito {

  val selectSql = "SELECT * FROM table;"

  val insertSql = "INSERT INTO table(id, name) VALUES(?, ?);"

  trait SQLDroidPreparedStatementScope extends Scope {

    val database = mock[SQLDroidDatabase]

    val sqliteDatabase = mock[SQLiteDatabase]

    database.database returns sqliteDatabase

    sqliteDatabase.isOpen returns true

    val connection: SQLDroidConnection = new SQLDroidConnection(databaseName = "databaseName", logWrapper = new TestLogWrapper) {
      override protected def createDatabase(): Option[SQLDroidDatabase] = Some(database)
    }

    val arguments = mock[PreparedStatementArguments]
  }

  trait WithoutConnection extends SQLDroidPreparedStatementScope {

    override val connection = javaNull

    val sqlDroid = new SQLDroidPreparedStatement(
      sql = insertSql,
      sqlDroidConnection = javaNull,
      columnGenerated = None,
      arguments = arguments,
      logWrapper = new TestLogWrapper())
  }

  trait WithConnection extends SQLDroidPreparedStatementScope {

    val columnGenerated = "ColumnGeneratedName"

    val sqlDroid = new SQLDroidPreparedStatement(
      sql = insertSql,
      sqlDroidConnection = connection,
      columnGenerated = Some(columnGenerated),
      arguments = arguments,
      logWrapper = new TestLogWrapper())
  }

  trait WithConnectionAndArgument extends SQLDroidPreparedStatementScope {

    val columnGenerated = "ColumnGeneratedName"

    override val arguments = new PreparedStatementArguments

    val sqlDroid = new SQLDroidPreparedStatement(
      sql = insertSql,
      sqlDroidConnection = connection,
      columnGenerated = Some(columnGenerated),
      arguments = arguments,
      logWrapper = new TestLogWrapper())
  }

  trait WithConnectionAndSelect extends SQLDroidPreparedStatementScope {

    val columnGenerated = "ColumnGeneratedName"

    val sqlDroid = new SQLDroidPreparedStatement(
      sql = selectSql,
      sqlDroidConnection = connection,
      columnGenerated = Some(columnGenerated),
      arguments = arguments,
      logWrapper = new TestLogWrapper())
  }

  trait WithMockLogger extends SQLDroidPreparedStatementScope {

    val columnGenerated = "ColumnGeneratedName"

    val mockLog = mock[LogWrapper]

    val sqlDroid = new SQLDroidPreparedStatement(
      sql = insertSql,
      sqlDroidConnection = connection,
      columnGenerated = Some(columnGenerated),
      arguments = arguments,
      logWrapper = mockLog)
  }

}

class SQLDroidPreparedStatementSpec
  extends SQLDroidPreparedStatementSpecification {

  "execute" should {

    "throws a SQLException when specify the sql argument" in new WithoutConnection {
      sqlDroid.execute(selectSql) must throwA[SQLException](sqlDroid.notInPreparedErrorMessage)
    }

    "throws a SQLException when specify the sql and autoGeneratedKeys argument" in new WithoutConnection {
      sqlDroid.execute(selectSql, 0) must throwA[SQLException](sqlDroid.notInPreparedErrorMessage)
    }

    "throws a SQLException when specify the sql and a column indexes argument" in new WithoutConnection {
      sqlDroid.execute(selectSql, scala.Array(0)) must throwA[SQLException](sqlDroid.notInPreparedErrorMessage)
    }

    "throws a SQLException when specify the sql and a column names argument" in new WithoutConnection {
      sqlDroid.execute(selectSql, scala.Array("")) must throwA[SQLException](sqlDroid.notInPreparedErrorMessage)
    }

    "throws a SQLException when the connection is close" in new WithoutConnection {
      sqlDroid.execute() must throwA[SQLException](sqlDroid.connectionClosedErrorMessage)
    }

    "return true with a select query and -1" in new WithConnectionAndSelect {
      val cursor = mock[Cursor]
      database.rawQuery(any, any) returns cursor
      sqlDroid.execute() must beTrue
      sqlDroid.getUpdateCount shouldEqual -1
    }

    "return false with a select query and the result of changedRowCount in database" in new WithConnection {
      val changedRowCount = 10
      database.changedRowCount() returns changedRowCount
      sqlDroid.execute() must beFalse
      sqlDroid.getUpdateCount shouldEqual changedRowCount
    }

  }

  "executeQuery" should {

    "throws a SQLException when specify the sql argument" in new WithoutConnection {
      sqlDroid.executeQuery(selectSql) must throwA[SQLException](sqlDroid.notInPreparedErrorMessage)
    }

    "return a SQLDroidResultSet with the Cursor returned by the database" in new WithConnectionAndSelect {
      val cursor = mock[Cursor]
      database.rawQuery(any, any) returns cursor
      val rs = sqlDroid.executeQuery()
      rs must beLike[ResultSet] {
        case st: SQLDroidResultSet => st.cursor shouldEqual cursor
      }
    }

  }

  "executeUpdate" should {

    "throws a SQLException when specify the sql argument" in new WithoutConnection {
      sqlDroid.executeUpdate(selectSql) must throwA[SQLException](sqlDroid.notInPreparedErrorMessage)
    }

    "throws a SQLException when specify the sql and autoGeneratedKeys argument" in new WithoutConnection {
      sqlDroid.executeUpdate(selectSql, 0) must throwA[SQLException](sqlDroid.notInPreparedErrorMessage)
    }

    "throws a SQLException when specify the sql and a column indexes argument" in new WithoutConnection {
      sqlDroid.executeUpdate(selectSql, scala.Array(0)) must throwA[SQLException](sqlDroid.notInPreparedErrorMessage)
    }

    "throws a SQLException when specify the sql and a column names argument" in new WithoutConnection {
      sqlDroid.executeUpdate(selectSql, scala.Array("")) must throwA[SQLException](sqlDroid.notInPreparedErrorMessage)
    }

    "return in both methods the result of changedRowCount in database" in new WithConnection {
      val changedRowCount = 10
      database.changedRowCount() returns changedRowCount
      sqlDroid.executeUpdate() shouldEqual changedRowCount
    }

  }

  "executeBatch" should {

    "return an array with one element when the batch is first initailized" in new WithConnectionAndArgument {
      sqlDroid.executeBatch() shouldEqual scala.Array(0)
    }

    "return an array with the results of changedRowCount and the sum when the bacth has elements" in new WithConnectionAndArgument {
      val batch = Seq(insertSql, insertSql, insertSql)
      val changedRowCounts = Seq(20, 30, 40)

      database.changedRowCount() returns(
        changedRowCounts.head,
        changedRowCounts.slice(1, changedRowCounts.size): _*)

      arguments.addNewEntry()
      arguments.addNewEntry()

      sqlDroid.executeBatch() shouldEqual changedRowCounts.toArray
    }

    "throws a SQLException when the connection is close" in new WithoutConnection {
      sqlDroid.executeBatch() must throwA[SQLException](sqlDroid.connectionClosedErrorMessage)
    }
  }

  "addBatch" should {

    "call to addNewEntry in arguments field when no specify params" in new WithoutConnection {
      sqlDroid.addBatch()
      there was one(arguments).addNewEntry()
    }

    "throws a SQLException when specify the sql argument" in new WithoutConnection {
      sqlDroid.addBatch(selectSql) must throwA[SQLException](sqlDroid.notInPreparedErrorMessage)
    }

  }

  "clearParameters" should {

    "call to clearArguments in arguments field" in new WithoutConnection {
      sqlDroid.clearParameters()
      there was one(arguments).clearArguments()
    }
  }

  "setBinaryStream" should {

    "call to setArgument in arguments field with the inputStream transformed into a byte array" in new WithConnection {
      val byteArray = "example".getBytes
      val inputStream = new ByteArrayInputStream(byteArray)
      sqlDroid.setBinaryStream(1, inputStream)
      there was one(arguments).setArgument(1, byteArray)
    }

    "call to setArgument in arguments field with the inputStream transformed into a byte array with the length specified" in
      new WithConnection {
        val byteArray = "example".getBytes
        val inputStream = new ByteArrayInputStream(byteArray)
        val length = 2
        sqlDroid.setBinaryStream(1, inputStream, length)
        there was one(arguments).setArgument(1, byteArray.slice(0, length))
      }

    "throws a SQLException when specify a negative length" in new WithoutConnection {
      val byteArray = "example".getBytes
      val inputStream = new ByteArrayInputStream(byteArray)
      sqlDroid.setBinaryStream(1, inputStream, -1) must throwA[SQLException]
    }

    "call to setObjectArgument with null when specify a null param" in new WithoutConnection {
      sqlDroid.setBinaryStream(1, javaNull)
      there was one(arguments).setObjectArgument(1, javaNull)
    }

    "throws a SQLException when specify a length greater than the max integer" in new WithoutConnection {
      val byteArray = "example".getBytes
      val inputStream = new ByteArrayInputStream(byteArray)
      sqlDroid.setBinaryStream(1, inputStream, Long.MaxValue) must throwA[SQLException]
    }

  }

  "setBlob" should {

    "call to setArgument in arguments field with the inputStream transformed into a byte array" in new WithConnection {
      val byteArray = "example".getBytes
      val inputStream = new ByteArrayInputStream(byteArray)
      sqlDroid.setBlob(1, inputStream)
      there was one(arguments).setArgument(1, byteArray)
    }

    "call to setArgument in arguments field with the inputStream transformed into a byte array with the length specified" in
      new WithConnection {
        val byteArray = "example".getBytes
        val inputStream = new ByteArrayInputStream(byteArray)
        val length = 2
        sqlDroid.setBlob(1, inputStream, length)
        there was one(arguments).setArgument(1, byteArray.slice(0, length))
      }

    "throws a SQLException when specify an InputStream and a negative length" in new WithoutConnection {
      val byteArray = "example".getBytes
      val inputStream = new ByteArrayInputStream(byteArray)
      sqlDroid.setBlob(1, inputStream, -1) must throwA[SQLException]
    }

    "call to setObjectArgument with null when specify a null InputStream param" in new WithoutConnection {
      sqlDroid.setBlob(1, javaNull.asInstanceOf[InputStream])
      there was one(arguments).setObjectArgument(1, javaNull)
    }

    "throws a SQLException when specify an InputStream and a length greater than the max integer" in new WithoutConnection {
      val byteArray = "example".getBytes
      val inputStream = new ByteArrayInputStream(byteArray)
      sqlDroid.setBlob(1, inputStream, Long.MaxValue) must throwA[SQLException]
    }

    "call to setArgument in arguments field with the Blob transformed into a byte array" in new WithConnection {
      val byteArray = "example".getBytes
      val inputStream = new ByteArrayInputStream(byteArray)
      val blob = mock[Blob]
      blob.length() returns byteArray.length
      blob.getBytes(1, byteArray.length) returns byteArray
      sqlDroid.setBlob(1, blob)
      there was one(arguments).setArgument(1, byteArray)
    }

    "call to setObjectArgument with null when specify a null Blob param" in new WithoutConnection {
      sqlDroid.setBlob(1, javaNull.asInstanceOf[Blob])
      there was one(arguments).setObjectArgument(1, javaNull)
    }

  }

  "setBoolean" should {

    "call to setArgument in arguments field with the boolean" in new WithConnection {
      val argument = true
      sqlDroid.setBoolean(1, argument)
      there was one(arguments).setArgument(1, argument)
    }

  }

  "setByte" should {

    "call to setArgument in arguments field with the byte" in new WithConnection {
      val argument = 1.toByte
      sqlDroid.setByte(1, argument)
      there was one(arguments).setArgument(1, argument)
    }

  }

  "setBytes" should {

    "call to setArgument in arguments field with the byte" in new WithConnection {
      val argument = scala.Array(0.toByte, 1.toByte)
      sqlDroid.setBytes(1, argument)
      there was one(arguments).setArgument(1, argument)
    }

  }

  "setClob" should {

    "call to setArgument in arguments field with the Reader transformed into a string" in new WithConnection {
      val string = "example"
      val reader = new StringReader(string)
      sqlDroid.setClob(1, reader)
      there was one(arguments).setArgument(1, string)
    }

    "call to setArgument in arguments field with the Reader transformed into a byte array with the length specified" in
      new WithConnection {
        val string = "example"
        val reader = new StringReader(string)
        val length = 2
        sqlDroid.setClob(1, reader, length)
        there was one(arguments).setArgument(1, string.substring(0, length))
      }

    "throws a SQLException when specify a Reader and a negative length" in new WithoutConnection {
      sqlDroid.setClob(1, new StringReader(""), -1) must throwA[SQLException]
    }

    "call to setObjectArgument with null when specify a null InputStream param" in new WithoutConnection {
      sqlDroid.setClob(1, javaNull.asInstanceOf[Reader])
      there was one(arguments).setObjectArgument(1, javaNull)
    }

    "throws a SQLException when specify a Reader and a length greater than the max integer" in new WithoutConnection {
      sqlDroid.setClob(1, new StringReader(""), Long.MaxValue) must throwA[SQLException]
    }

    "call to setArgument in arguments field with the Clob transformed into a string" in new WithConnection {
      val string = "example"
      val clob = mock[Clob]
      clob.length() returns string.length
      clob.getSubString(1, string.length) returns string
      sqlDroid.setClob(1, clob)
      there was one(arguments).setArgument(1, string)
    }

    "throws a SQLException when the length of the clob is greater than the max integer" in new WithConnection {
      val clob = mock[Clob]
      clob.length() returns Long.MaxValue
      sqlDroid.setClob(1, clob) must throwA[SQLException]
    }

    "call to setObjectArgument with null when specify a null Clob param" in new WithoutConnection {
      sqlDroid.setClob(1, javaNull.asInstanceOf[Clob])
      there was one(arguments).setObjectArgument(1, javaNull)
    }

  }

  "setDate" should {

    "call to setArgument in arguments field with the Date" in new WithConnection {
      val date = new java.sql.Date(System.currentTimeMillis())
      sqlDroid.setDate(1, date)
      there was one(arguments).setArgument(1, date)
    }

    "call to setArgument in arguments field with the Date when passing a Calendar instance" in
      new WithConnection {
        val date = new java.sql.Date(System.currentTimeMillis())
        sqlDroid.setDate(1, date, mock[Calendar])
        there was one(arguments).setArgument(1, date)
      }

  }

  "setDouble" should {

    "call to setArgument in arguments field with the Double" in new WithConnection {
      val argument = 1.toDouble
      sqlDroid.setDouble(1, argument)
      there was one(arguments).setArgument(1, argument)
    }

  }

  "setFloat" should {

    "call to setArgument in arguments field with the Float" in new WithConnection {
      val argument = 1.toFloat
      sqlDroid.setFloat(1, argument)
      there was one(arguments).setArgument(1, argument)
    }

  }

  "setInt" should {

    "call to setArgument in arguments field with the Int" in new WithConnection {
      val argument = 1
      sqlDroid.setInt(1, argument)
      there was one(arguments).setArgument(1, argument)
    }

  }

  "setLong" should {

    "call to setArgument in arguments field with the Long" in new WithConnection {
      val argument = 1.toLong
      sqlDroid.setLong(1, argument)
      there was one(arguments).setArgument(1, argument)
    }

  }

  "setNull" should {

    "call to setObjectArgument in arguments field with a null value when specify field type" in
      new WithConnection {
        sqlDroid.setNull(1, 1)
        there was one(arguments).setObjectArgument(1, javaNull)
      }

    "call to setObjectArgument in arguments field with a null value when specify field type and field name" in
      new WithConnection {
        sqlDroid.setNull(1, 1, "")
        there was one(arguments).setObjectArgument(1, javaNull)
      }

  }

  "setObject" should {

    "call to setObjectArgument in arguments field with the value" in
      new WithConnection {
        val argument: Any = ""
        sqlDroid.setObject(1, argument)
        there was one(arguments).setObjectArgument(1, argument)
      }

    "call to setObjectArgument in arguments field with the value when specify field type" in
      new WithConnection {
        val argument: Any = ""
        sqlDroid.setObject(1, argument, 1)
        there was one(arguments).setObjectArgument(1, argument)
      }

    "call to setObjectArgument in arguments field with the value when specify field type and the scale" in
      new WithConnection {
        val argument: Any = ""
        sqlDroid.setObject(1, argument, 1, 1)
        there was one(arguments).setObjectArgument(1, argument)
      }

  }

  "setShort" should {

    "call to setArgument in arguments field with the Short" in new WithConnection {
      val argument = 1.toShort
      sqlDroid.setShort(1, argument)
      there was one(arguments).setArgument(1, argument)
    }

  }

  "setString" should {

    "call to setArgument in arguments field with the String" in new WithConnection {
      val argument = 1.toString
      sqlDroid.setString(1, argument)
      there was one(arguments).setArgument(1, argument)
    }

  }

  "setTime" should {

    "call to setArgument in arguments field with the Time" in new WithConnection {
      val time = new java.sql.Time(System.currentTimeMillis())
      sqlDroid.setTime(1, time)
      there was one(arguments).setArgument(1, time)
    }

    "call to setArgument in arguments field with the Time when passing a Calendar instance" in
      new WithConnection {
        val time = new java.sql.Time(System.currentTimeMillis())
        sqlDroid.setTime(1, time, mock[Calendar])
        there was one(arguments).setArgument(1, time)
      }

  }

  "setTimestamp" should {

    "call to setArgument in arguments field with the Timestamp" in new WithConnection {
      val timestamp = new java.sql.Timestamp(System.currentTimeMillis())
      sqlDroid.setTimestamp(1, timestamp)
      there was one(arguments).setArgument(1, timestamp)
    }

    "call to setArgument in arguments field with the Timestamp when passing a Calendar instance" in
      new WithConnection {
        val timestamp = new java.sql.Timestamp(System.currentTimeMillis())
        sqlDroid.setTimestamp(1, timestamp, mock[Calendar])
        there was one(arguments).setArgument(1, timestamp)
      }

  }

  "getMetaData" should {

    "call to LoggerWrapper.notImplemented" in new WithMockLogger {
      sqlDroid.getMetaData
      there was one(mockLog).notImplemented(javaNull)
    }

  }

  "getParameterMetaData" should {

    "call to LoggerWrapper.notImplemented" in new WithMockLogger {
      sqlDroid.getParameterMetaData
      there was one(mockLog).notImplemented(javaNull)
    }

  }

  "setArray" should {

    "call to LoggerWrapper.notImplemented" in new WithMockLogger {
      sqlDroid.setArray(1, javaNull)
      there was one(mockLog).notImplemented(Unit)
    }

  }

  "setAsciiStream" should {

    "call to LoggerWrapper.notImplemented" in new WithMockLogger {
      sqlDroid.setAsciiStream(1, javaNull)
      there was one(mockLog).notImplemented(Unit)
    }

  }

  "setAsciiStream" should {

    "call to LoggerWrapper.notImplemented" in new WithMockLogger {
      sqlDroid.setAsciiStream(1, javaNull)
      there was one(mockLog).notImplemented(Unit)
    }

    "call to LoggerWrapper.notImplemented when passing length argument" in
      new WithMockLogger {
        sqlDroid.setAsciiStream(1, javaNull, 1l)
        there was one(mockLog).notImplemented(Unit)
      }

  }

  "setBigDecimal" should {

    "call to LoggerWrapper.notImplemented" in new WithMockLogger {
      sqlDroid.setBigDecimal(1, javaNull)
      there was one(mockLog).notImplemented(Unit)
    }

  }

  "setCharacterStream" should {

    "call to LoggerWrapper.notImplemented" in new WithMockLogger {
      sqlDroid.setCharacterStream(1, javaNull)
      there was one(mockLog).notImplemented(Unit)
    }

    "call to LoggerWrapper.notImplemented when passing length argument" in
      new WithMockLogger {
      sqlDroid.setCharacterStream(1, javaNull, 1)
      there was one(mockLog).notImplemented(Unit)
    }

    "call to LoggerWrapper.notImplemented when passing length argument as long" in
      new WithMockLogger {
      sqlDroid.setCharacterStream(1, javaNull, 1l)
      there was one(mockLog).notImplemented(Unit)
    }

  }

  "setNCharacterStream" should {

    "call to LoggerWrapper.notImplemented" in new WithMockLogger {
      sqlDroid.setNCharacterStream(1, javaNull)
      there was one(mockLog).notImplemented(Unit)
    }

    "call to LoggerWrapper.notImplemented when passing length argument" in
      new WithMockLogger {
      sqlDroid.setNCharacterStream(1, javaNull, 1l)
      there was one(mockLog).notImplemented(Unit)
    }

  }

  "setNClob" should {

    "call to LoggerWrapper.notImplemented" in new WithMockLogger {
      sqlDroid.setNClob(1, javaNull.asInstanceOf[Reader])
      there was one(mockLog).notImplemented(Unit)
    }

    "call to LoggerWrapper.notImplemented when passing length argument" in
      new WithMockLogger {
      sqlDroid.setNClob(1, javaNull.asInstanceOf[Reader], 1)
      there was one(mockLog).notImplemented(Unit)
    }

    "call to LoggerWrapper.notImplemented when passing a NClob" in new WithMockLogger {
      sqlDroid.setNClob(1, javaNull.asInstanceOf[NClob])
      there was one(mockLog).notImplemented(Unit)
    }

  }

  "setNString" should {

    "call to LoggerWrapper.notImplemented" in new WithMockLogger {
      sqlDroid.setNString(1, javaNull)
      there was one(mockLog).notImplemented(Unit)
    }

  }

  "setRef" should {

    "call to LoggerWrapper.notImplemented" in new WithMockLogger {
      sqlDroid.setRef(1, javaNull)
      there was one(mockLog).notImplemented(Unit)
    }

  }

  "setRowId" should {

    "call to LoggerWrapper.notImplemented" in new WithMockLogger {
      sqlDroid.setRowId(1, javaNull)
      there was one(mockLog).notImplemented(Unit)
    }

  }

  "setSQLXML" should {

    "call to LoggerWrapper.notImplemented" in new WithMockLogger {
      sqlDroid.setSQLXML(1, javaNull)
      there was one(mockLog).notImplemented(Unit)
    }

  }

  "setURL" should {

    "call to LoggerWrapper.notImplemented" in new WithMockLogger {
      sqlDroid.setURL(1, javaNull)
      there was one(mockLog).notImplemented(Unit)
    }

  }

  "setUnicodeStream" should {

    "call to LoggerWrapper.notImplemented" in new WithMockLogger {
      sqlDroid.setUnicodeStream(1, javaNull, 1)
      there was one(mockLog).notImplemented(Unit)
    }

  }

}
