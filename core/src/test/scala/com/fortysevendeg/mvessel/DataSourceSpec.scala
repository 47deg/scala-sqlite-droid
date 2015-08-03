package com.fortysevendeg.mvessel

import java.util.Properties

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.util.Random

trait DataSourceSpecification
  extends Specification
  with Mockito {

  trait DataSourceScope
    extends Scope {

    val driver = mock[Driver]

    val connection = mock[Connection]

    val properties = new Properties

    val dbPath = "/path/dbname.db"

    val datasource = new DataSource(driver, properties, dbPath, new TestLogWrapper)

  }

}

class DataSourceSpec
  extends DataSourceSpecification {

  "getConnection" should {

    "call to connect method of driver with right parameters" in
      new DataSourceScope {

        driver.connect(contain(dbPath), beTheSameAs(properties)).returns(connection)

        datasource.getConnection shouldEqual connection
      }

    "call to connect method of driver with right parameters when passing username and password" in
      new DataSourceScope {

        driver.connect(contain(dbPath), beTheSameAs(properties)).returns(connection)

        datasource.getConnection(Random.nextString(10), Random.nextString(10)) shouldEqual connection
      }

  }

  "getParentLogger" should {

    "returns a java null" in new DataSourceScope {
      datasource.getParentLogger shouldEqual javaNull
    }

  }

  "getLoginTimeout" should {

    "returns 0" in new DataSourceScope {
      datasource.getLoginTimeout shouldEqual 0
    }

  }

  "isWrapperFor" should {

    "throws an UnsupportedOperationException" in new DataSourceScope {
      datasource.isWrapperFor(classOf[DataSource]) must throwA[UnsupportedOperationException]
    }

  }

  "unwrap" should {

    "throws an UnsupportedOperationException" in new DataSourceScope {
      datasource.unwrap(classOf[DataSource]) must throwA[UnsupportedOperationException]
    }

  }

}