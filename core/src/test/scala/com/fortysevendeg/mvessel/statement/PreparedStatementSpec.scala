package com.fortysevendeg.mvessel.statement

import java.io.{ByteArrayInputStream, InputStream, Reader, StringReader}
import java.sql.{Blob, Clob, NClob, ResultSet => SQLResultSet, SQLException}
import java.util.Calendar

import com.fortysevendeg.mvessel._
import com.fortysevendeg.mvessel.api.{CursorProxy, DatabaseProxyFactory, DatabaseProxy}
import com.fortysevendeg.mvessel.logging.LogWrapper
import com.fortysevendeg.mvessel.resultset.ResultSet
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

trait PreparedStatementSpecification
  extends Specification
  with Mockito {

  val selectSql = "SELECT * FROM table;"

  val insertSql = "INSERT INTO table(id, name) VALUES(?, ?);"

  trait PreparedStatementScope extends Scope {

    val databaseName = "databaseName"

    val logWrapper = new TestLogWrapper

    val databaseProxy = mock[DatabaseProxy[CursorProxy]]

    val databaseProxyFactory = mock[DatabaseProxyFactory[CursorProxy]]

    databaseProxyFactory.openDatabase(any, any) returns databaseProxy

    databaseProxy.isOpen returns true

    val connection: Connection[CursorProxy] = new Connection(
      databaseWrapperFactory = databaseProxyFactory,
      databaseName = databaseName,
      logWrapper = logWrapper)

    val arguments = mock[PreparedStatementArguments]
  }

  trait WithoutConnection extends PreparedStatementScope {

    override val connection = javaNull

    val preparedStatement = new PreparedStatement(
      sql = insertSql,
      connection = javaNull,
      columnGenerated = None,
      arguments = arguments,
      logWrapper = new TestLogWrapper())
  }

  trait WithConnection extends PreparedStatementScope {

    val columnGenerated = "ColumnGeneratedName"

    val preparedStatement = new PreparedStatement(
      sql = insertSql,
      connection = connection,
      columnGenerated = Some(columnGenerated),
      arguments = arguments,
      logWrapper = new TestLogWrapper())
  }

  trait WithConnectionAndArgument extends PreparedStatementScope {

    val columnGenerated = "ColumnGeneratedName"

    override val arguments = new PreparedStatementArguments

    val preparedStatement = new PreparedStatement(
      sql = insertSql,
      connection = connection,
      columnGenerated = Some(columnGenerated),
      arguments = arguments,
      logWrapper = new TestLogWrapper())
  }

  trait WithConnectionAndSelect extends PreparedStatementScope {

    val columnGenerated = "ColumnGeneratedName"

    val preparedStatement = new PreparedStatement(
      sql = selectSql,
      connection = connection,
      columnGenerated = Some(columnGenerated),
      arguments = arguments,
      logWrapper = new TestLogWrapper())
  }

  trait WithMockLogger extends PreparedStatementScope {

    val columnGenerated = "ColumnGeneratedName"

    val mockLog = mock[LogWrapper]

    val preparedStatement = new PreparedStatement[CursorProxy](
      sql = insertSql,
      connection = connection,
      columnGenerated = Some(columnGenerated),
      arguments = arguments,
      logWrapper = mockLog)
  }

}

class PreparedStatementSpec
  extends PreparedStatementSpecification {

  "execute" should {

    "throws a SQLException when specify the sql argument" in new WithoutConnection {
      preparedStatement.execute(selectSql) must throwA[SQLException](preparedStatement.notInPreparedErrorMessage)
    }

    "throws a SQLException when specify the sql and autoGeneratedKeys argument" in new WithoutConnection {
      preparedStatement.execute(selectSql, 0) must throwA[SQLException](preparedStatement.notInPreparedErrorMessage)
    }

    "throws a SQLException when specify the sql and a column indexes argument" in new WithoutConnection {
      preparedStatement.execute(selectSql, scala.Array(0)) must throwA[SQLException](preparedStatement.notInPreparedErrorMessage)
    }

    "throws a SQLException when specify the sql and a column names argument" in new WithoutConnection {
      preparedStatement.execute(selectSql, scala.Array("")) must throwA[SQLException](preparedStatement.notInPreparedErrorMessage)
    }

    "throws a SQLException when the connection is close" in new WithoutConnection {
      preparedStatement.execute() must throwA[SQLException](preparedStatement.connectionClosedErrorMessage)
    }

    "return true with a select query and -1" in new WithConnectionAndSelect {
      val cursor = mock[CursorProxy]
      databaseProxy.rawQuery(any, any) returns cursor
      preparedStatement.execute() must beTrue
      preparedStatement.getUpdateCount shouldEqual -1
    }

    "return false with a select query and the result of changedRowCount in database" in new WithConnection {
      val changedRowCount = 10
      databaseProxy.changedRowCount returns Some(changedRowCount)
      preparedStatement.execute() must beFalse
      preparedStatement.getUpdateCount shouldEqual changedRowCount
    }

  }

  "executeQuery" should {

    "throws a SQLException when specify the sql argument" in new WithoutConnection {
      preparedStatement.executeQuery(selectSql) must throwA[SQLException](preparedStatement.notInPreparedErrorMessage)
    }

    "return a ResultSet with the Cursor returned by the database" in new WithConnectionAndSelect {
      val cursor = mock[CursorProxy]
      databaseProxy.rawQuery(any, any) returns cursor
      val rs = preparedStatement.executeQuery()
      rs must beLike[SQLResultSet] {
        case st: ResultSet[_] => st.cursor shouldEqual cursor
      }
    }

  }

  "executeUpdate" should {

    "throws a SQLException when specify the sql argument" in new WithoutConnection {
      preparedStatement.executeUpdate(selectSql) must throwA[SQLException](preparedStatement.notInPreparedErrorMessage)
    }

    "throws a SQLException when specify the sql and autoGeneratedKeys argument" in new WithoutConnection {
      preparedStatement.executeUpdate(selectSql, 0) must throwA[SQLException](preparedStatement.notInPreparedErrorMessage)
    }

    "throws a SQLException when specify the sql and a column indexes argument" in new WithoutConnection {
      preparedStatement.executeUpdate(selectSql, scala.Array(0)) must throwA[SQLException](preparedStatement.notInPreparedErrorMessage)
    }

    "throws a SQLException when specify the sql and a column names argument" in new WithoutConnection {
      preparedStatement.executeUpdate(selectSql, scala.Array("")) must throwA[SQLException](preparedStatement.notInPreparedErrorMessage)
    }

    "return in both methods the result of changedRowCount in database" in new WithConnection {
      val changedRowCount = 10
      databaseProxy.changedRowCount returns Some(changedRowCount)
      preparedStatement.executeUpdate() shouldEqual changedRowCount
    }

  }

  "executeBatch" should {

    "return an array with one element when the batch is first initialized" in new WithConnectionAndArgument {
      databaseProxy.changedRowCount returns Some(0)
      preparedStatement.executeBatch() shouldEqual scala.Array(0)
    }

    "return an array with the results of changedRowCount and the sum when the bacth has elements" in new WithConnectionAndArgument {
      val batch = Seq(insertSql, insertSql, insertSql)
      val changedRowCounts = Seq(Some(20), Some(30), Some(40))

      databaseProxy.changedRowCount returns(
        changedRowCounts.head,
        changedRowCounts.slice(1, changedRowCounts.size): _*)

      arguments.addNewEntry()
      arguments.addNewEntry()

      preparedStatement.executeBatch() shouldEqual changedRowCounts.map(_.get).toArray
    }

    "throws a SQLException when the connection is close" in new WithoutConnection {
      preparedStatement.executeBatch() must throwA[SQLException](preparedStatement.connectionClosedErrorMessage)
    }
  }

  "addBatch" should {

    "call to addNewEntry in arguments field when no specify params" in new WithoutConnection {
      preparedStatement.addBatch()
      there was one(arguments).addNewEntry()
    }

    "throws a SQLException when specify the sql argument" in new WithoutConnection {
      preparedStatement.addBatch(selectSql) must throwA[SQLException](preparedStatement.notInPreparedErrorMessage)
    }

  }

  "clearParameters" should {

    "call to clearArguments in arguments field" in new WithoutConnection {
      preparedStatement.clearParameters()
      there was one(arguments).clearArguments()
    }
  }

  "setBinaryStream" should {

    "call to setArgument in arguments field with the inputStream transformed into a byte array" in new WithConnection {
      val byteArray = "example".getBytes
      val inputStream = new ByteArrayInputStream(byteArray)
      preparedStatement.setBinaryStream(1, inputStream)
      there was one(arguments).setArgument(1, byteArray)
    }

    "call to setArgument in arguments field with the inputStream transformed into a byte array with the length specified" in
      new WithConnection {
        val byteArray = "example".getBytes
        val inputStream = new ByteArrayInputStream(byteArray)
        val length = 2
        preparedStatement.setBinaryStream(1, inputStream, length)
        there was one(arguments).setArgument(1, byteArray.slice(0, length))
      }

    "throws a SQLException when specify a negative length" in new WithoutConnection {
      val byteArray = "example".getBytes
      val inputStream = new ByteArrayInputStream(byteArray)
      preparedStatement.setBinaryStream(1, inputStream, -1) must throwA[SQLException]
    }

    "call to setObjectArgument with null when specify a null param" in new WithoutConnection {
      preparedStatement.setBinaryStream(1, javaNull)
      there was one(arguments).setObjectArgument(1, javaNull)
    }

    "throws a SQLException when specify a length greater than the max integer" in new WithoutConnection {
      val byteArray = "example".getBytes
      val inputStream = new ByteArrayInputStream(byteArray)
      preparedStatement.setBinaryStream(1, inputStream, Long.MaxValue) must throwA[SQLException]
    }

  }

  "setBlob" should {

    "call to setArgument in arguments field with the inputStream transformed into a byte array" in new WithConnection {
      val byteArray = "example".getBytes
      val inputStream = new ByteArrayInputStream(byteArray)
      preparedStatement.setBlob(1, inputStream)
      there was one(arguments).setArgument(1, byteArray)
    }

    "call to setArgument in arguments field with the inputStream transformed into a byte array with the length specified" in
      new WithConnection {
        val byteArray = "example".getBytes
        val inputStream = new ByteArrayInputStream(byteArray)
        val length = 2
        preparedStatement.setBlob(1, inputStream, length)
        there was one(arguments).setArgument(1, byteArray.slice(0, length))
      }

    "throws a SQLException when specify an InputStream and a negative length" in new WithoutConnection {
      val byteArray = "example".getBytes
      val inputStream = new ByteArrayInputStream(byteArray)
      preparedStatement.setBlob(1, inputStream, -1) must throwA[SQLException]
    }

    "call to setObjectArgument with null when specify a null InputStream param" in new WithoutConnection {
      preparedStatement.setBlob(1, javaNull.asInstanceOf[InputStream])
      there was one(arguments).setObjectArgument(1, javaNull)
    }

    "throws a SQLException when specify an InputStream and a length greater than the max integer" in new WithoutConnection {
      val byteArray = "example".getBytes
      val inputStream = new ByteArrayInputStream(byteArray)
      preparedStatement.setBlob(1, inputStream, Long.MaxValue) must throwA[SQLException]
    }

    "call to setArgument in arguments field with the Blob transformed into a byte array" in new WithConnection {
      val byteArray = "example".getBytes
      val inputStream = new ByteArrayInputStream(byteArray)
      val blob = mock[Blob]
      blob.length() returns byteArray.length
      blob.getBytes(1, byteArray.length) returns byteArray
      preparedStatement.setBlob(1, blob)
      there was one(arguments).setArgument(1, byteArray)
    }

    "call to setObjectArgument with null when specify a null Blob param" in new WithoutConnection {
      preparedStatement.setBlob(1, javaNull.asInstanceOf[Blob])
      there was one(arguments).setObjectArgument(1, javaNull)
    }

  }

  "setBoolean" should {

    "call to setArgument in arguments field with the boolean" in new WithConnection {
      val argument = true
      preparedStatement.setBoolean(1, argument)
      there was one(arguments).setArgument(1, argument)
    }

  }

  "setByte" should {

    "call to setArgument in arguments field with the byte" in new WithConnection {
      val argument = 1.toByte
      preparedStatement.setByte(1, argument)
      there was one(arguments).setArgument(1, argument)
    }

  }

  "setBytes" should {

    "call to setArgument in arguments field with the byte" in new WithConnection {
      val argument = scala.Array(0.toByte, 1.toByte)
      preparedStatement.setBytes(1, argument)
      there was one(arguments).setArgument(1, argument)
    }

  }

  "setClob" should {

    "call to setArgument in arguments field with the Reader transformed into a string" in new WithConnection {
      val string = "example"
      val reader = new StringReader(string)
      preparedStatement.setClob(1, reader)
      there was one(arguments).setArgument(1, string)
    }

    "call to setArgument in arguments field with the Reader transformed into a byte array with the length specified" in
      new WithConnection {
        val string = "example"
        val reader = new StringReader(string)
        val length = 2
        preparedStatement.setClob(1, reader, length)
        there was one(arguments).setArgument(1, string.substring(0, length))
      }

    "throws a SQLException when specify a Reader and a negative length" in new WithoutConnection {
      preparedStatement.setClob(1, new StringReader(""), -1) must throwA[SQLException]
    }

    "call to setObjectArgument with null when specify a null InputStream param" in new WithoutConnection {
      preparedStatement.setClob(1, javaNull.asInstanceOf[Reader])
      there was one(arguments).setObjectArgument(1, javaNull)
    }

    "throws a SQLException when specify a Reader and a length greater than the max integer" in new WithoutConnection {
      preparedStatement.setClob(1, new StringReader(""), Long.MaxValue) must throwA[SQLException]
    }

    "call to setArgument in arguments field with the Clob transformed into a string" in new WithConnection {
      val string = "example"
      val clob = mock[Clob]
      clob.length() returns string.length
      clob.getSubString(1, string.length) returns string
      preparedStatement.setClob(1, clob)
      there was one(arguments).setArgument(1, string)
    }

    "throws a SQLException when the length of the clob is greater than the max integer" in new WithConnection {
      val clob = mock[Clob]
      clob.length() returns Long.MaxValue
      preparedStatement.setClob(1, clob) must throwA[SQLException]
    }

    "call to setObjectArgument with null when specify a null Clob param" in new WithoutConnection {
      preparedStatement.setClob(1, javaNull.asInstanceOf[Clob])
      there was one(arguments).setObjectArgument(1, javaNull)
    }

  }

  "setDate" should {

    "call to setArgument in arguments field with the Date" in new WithConnection {
      val date = new java.sql.Date(System.currentTimeMillis())
      preparedStatement.setDate(1, date)
      there was one(arguments).setArgument(1, date)
    }

    "call to setArgument in arguments field with the Date when passing a Calendar instance" in
      new WithConnection {
        val date = new java.sql.Date(System.currentTimeMillis())
        preparedStatement.setDate(1, date, mock[Calendar])
        there was one(arguments).setArgument(1, date)
      }

  }

  "setDouble" should {

    "call to setArgument in arguments field with the Double" in new WithConnection {
      val argument = 1.toDouble
      preparedStatement.setDouble(1, argument)
      there was one(arguments).setArgument(1, argument)
    }

  }

  "setFloat" should {

    "call to setArgument in arguments field with the Float" in new WithConnection {
      val argument = 1.toFloat
      preparedStatement.setFloat(1, argument)
      there was one(arguments).setArgument(1, argument)
    }

  }

  "setInt" should {

    "call to setArgument in arguments field with the Int" in new WithConnection {
      val argument = 1
      preparedStatement.setInt(1, argument)
      there was one(arguments).setArgument(1, argument)
    }

  }

  "setLong" should {

    "call to setArgument in arguments field with the Long" in new WithConnection {
      val argument = 1.toLong
      preparedStatement.setLong(1, argument)
      there was one(arguments).setArgument(1, argument)
    }

  }

  "setNull" should {

    "call to setObjectArgument in arguments field with a null value when specify field type" in
      new WithConnection {
        preparedStatement.setNull(1, 1)
        there was one(arguments).setObjectArgument(1, javaNull)
      }

    "call to setObjectArgument in arguments field with a null value when specify field type and field name" in
      new WithConnection {
        preparedStatement.setNull(1, 1, "")
        there was one(arguments).setObjectArgument(1, javaNull)
      }

  }

  "setObject" should {

    "call to setObjectArgument in arguments field with the value" in
      new WithConnection {
        val argument: Any = ""
        preparedStatement.setObject(1, argument)
        there was one(arguments).setObjectArgument(1, argument)
      }

    "call to setObjectArgument in arguments field with the value when specify field type" in
      new WithConnection {
        val argument: Any = ""
        preparedStatement.setObject(1, argument, 1)
        there was one(arguments).setObjectArgument(1, argument)
      }

    "call to setObjectArgument in arguments field with the value when specify field type and the scale" in
      new WithConnection {
        val argument: Any = ""
        preparedStatement.setObject(1, argument, 1, 1)
        there was one(arguments).setObjectArgument(1, argument)
      }

  }

  "setShort" should {

    "call to setArgument in arguments field with the Short" in new WithConnection {
      val argument = 1.toShort
      preparedStatement.setShort(1, argument)
      there was one(arguments).setArgument(1, argument)
    }

  }

  "setString" should {

    "call to setArgument in arguments field with the String" in new WithConnection {
      val argument = 1.toString
      preparedStatement.setString(1, argument)
      there was one(arguments).setArgument(1, argument)
    }

  }

  "setTime" should {

    "call to setArgument in arguments field with the Time" in new WithConnection {
      val time = new java.sql.Time(System.currentTimeMillis())
      preparedStatement.setTime(1, time)
      there was one(arguments).setArgument(1, time)
    }

    "call to setArgument in arguments field with the Time when passing a Calendar instance" in
      new WithConnection {
        val time = new java.sql.Time(System.currentTimeMillis())
        preparedStatement.setTime(1, time, mock[Calendar])
        there was one(arguments).setArgument(1, time)
      }

  }

  "setTimestamp" should {

    "call to setArgument in arguments field with the Timestamp" in new WithConnection {
      val timestamp = new java.sql.Timestamp(System.currentTimeMillis())
      preparedStatement.setTimestamp(1, timestamp)
      there was one(arguments).setArgument(1, timestamp)
    }

    "call to setArgument in arguments field with the Timestamp when passing a Calendar instance" in
      new WithConnection {
        val timestamp = new java.sql.Timestamp(System.currentTimeMillis())
        preparedStatement.setTimestamp(1, timestamp, mock[Calendar])
        there was one(arguments).setArgument(1, timestamp)
      }

  }

  "getMetaData" should {

    "call to LoggerWrapper.notImplemented" in new WithMockLogger {
      preparedStatement.getMetaData
      there was one(mockLog).notImplemented(javaNull)
    }

  }

  "getParameterMetaData" should {

    "call to LoggerWrapper.notImplemented" in new WithMockLogger {
      preparedStatement.getParameterMetaData
      there was one(mockLog).notImplemented(javaNull)
    }

  }

  "setArray" should {

    "call to LoggerWrapper.notImplemented" in new WithMockLogger {
      preparedStatement.setArray(1, javaNull)
      there was one(mockLog).notImplemented(Unit)
    }

  }

  "setAsciiStream" should {

    "call to LoggerWrapper.notImplemented" in new WithMockLogger {
      preparedStatement.setAsciiStream(1, javaNull)
      there was one(mockLog).notImplemented(Unit)
    }

  }

  "setAsciiStream" should {

    "call to LoggerWrapper.notImplemented" in new WithMockLogger {
      preparedStatement.setAsciiStream(1, javaNull)
      there was one(mockLog).notImplemented(Unit)
    }

    "call to LoggerWrapper.notImplemented when passing length argument" in
      new WithMockLogger {
        preparedStatement.setAsciiStream(1, javaNull, 1l)
        there was one(mockLog).notImplemented(Unit)
      }

  }

  "setBigDecimal" should {

    "call to LoggerWrapper.notImplemented" in new WithMockLogger {
      preparedStatement.setBigDecimal(1, javaNull)
      there was one(mockLog).notImplemented(Unit)
    }

  }

  "setCharacterStream" should {

    "call to LoggerWrapper.notImplemented" in new WithMockLogger {
      preparedStatement.setCharacterStream(1, javaNull)
      there was one(mockLog).notImplemented(Unit)
    }

    "call to LoggerWrapper.notImplemented when passing length argument" in
      new WithMockLogger {
      preparedStatement.setCharacterStream(1, javaNull, 1)
      there was one(mockLog).notImplemented(Unit)
    }

    "call to LoggerWrapper.notImplemented when passing length argument as long" in
      new WithMockLogger {
      preparedStatement.setCharacterStream(1, javaNull, 1l)
      there was one(mockLog).notImplemented(Unit)
    }

  }

  "setNCharacterStream" should {

    "call to LoggerWrapper.notImplemented" in new WithMockLogger {
      preparedStatement.setNCharacterStream(1, javaNull)
      there was one(mockLog).notImplemented(Unit)
    }

    "call to LoggerWrapper.notImplemented when passing length argument" in
      new WithMockLogger {
      preparedStatement.setNCharacterStream(1, javaNull, 1l)
      there was one(mockLog).notImplemented(Unit)
    }

  }

  "setNClob" should {

    "call to LoggerWrapper.notImplemented" in new WithMockLogger {
      preparedStatement.setNClob(1, javaNull.asInstanceOf[Reader])
      there was one(mockLog).notImplemented(Unit)
    }

    "call to LoggerWrapper.notImplemented when passing length argument" in
      new WithMockLogger {
      preparedStatement.setNClob(1, javaNull.asInstanceOf[Reader], 1)
      there was one(mockLog).notImplemented(Unit)
    }

    "call to LoggerWrapper.notImplemented when passing a NClob" in new WithMockLogger {
      preparedStatement.setNClob(1, javaNull.asInstanceOf[NClob])
      there was one(mockLog).notImplemented(Unit)
    }

  }

  "setNString" should {

    "call to LoggerWrapper.notImplemented" in new WithMockLogger {
      preparedStatement.setNString(1, javaNull)
      there was one(mockLog).notImplemented(Unit)
    }

  }

  "setRef" should {

    "call to LoggerWrapper.notImplemented" in new WithMockLogger {
      preparedStatement.setRef(1, javaNull)
      there was one(mockLog).notImplemented(Unit)
    }

  }

  "setRowId" should {

    "call to LoggerWrapper.notImplemented" in new WithMockLogger {
      preparedStatement.setRowId(1, javaNull)
      there was one(mockLog).notImplemented(Unit)
    }

  }

  "setSQLXML" should {

    "call to LoggerWrapper.notImplemented" in new WithMockLogger {
      preparedStatement.setSQLXML(1, javaNull)
      there was one(mockLog).notImplemented(Unit)
    }

  }

  "setURL" should {

    "call to LoggerWrapper.notImplemented" in new WithMockLogger {
      preparedStatement.setURL(1, javaNull)
      there was one(mockLog).notImplemented(Unit)
    }

  }

  "setUnicodeStream" should {

    "call to LoggerWrapper.notImplemented" in new WithMockLogger {
      preparedStatement.setUnicodeStream(1, javaNull, 1)
      there was one(mockLog).notImplemented(Unit)
    }

  }

}
