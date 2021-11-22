package com.oracle.infy.wookiee.srcgen
import Example._
import Example2._
import com.oracle.infy.wookiee.grpc.srcgen.testService.testService._
import scala.util.Try
import java.time._

object implicits {

  private def fromGrpcZonedDateTime(value: Long): Either[GrpcConversionError, ZonedDateTime] =
    Try {
      ZonedDateTime.ofInstant(Instant.ofEpochSecond(value), ZoneId.of("UTC"))
    }.toEither.left.map(t => GrpcConversionError(t.getMessage))

  private def toGrpcZonedDateTime(value: ZonedDateTime): Long =
    value.toEpochSecond
  locally {
    val _ = (a => fromGrpcZonedDateTime(a), a => toGrpcZonedDateTime(a))
  }

  implicit class ASErrorToGrpc(lhs: ASError) {

    def toGrpc: GrpcASError = lhs match {
      case value: DestinationError =>
        GrpcASError(GrpcASError.OneOf.DestinationError(value.toGrpc))
      case value: ConnectionError =>
        GrpcASError(GrpcASError.OneOf.ConnectionError(value.toGrpc))
      case _ =>
        GrpcASError(GrpcASError.OneOf.Empty)
    }
  }

  implicit class ASErrorFromGrpc(lhs: GrpcASError) {

    def fromGrpc: Either[GrpcConversionError, ASError] = lhs.oneOf match {
      case GrpcASError.OneOf.Empty =>
        Left(GrpcConversionError("Unable to convert object from grpc type: GrpcASError"))
      case GrpcASError.OneOf.DestinationError(value) =>
        value.fromGrpc
      case GrpcASError.OneOf.ConnectionError(value) =>
        value.fromGrpc
    }
  }

  implicit class DestinationErrorToGrpc(lhs: DestinationError) {

    def toGrpc: GrpcDestinationError = lhs match {
      case value: MaxyDestinationValidationError =>
        GrpcDestinationError(GrpcDestinationError.OneOf.MaxyDestinationValidationError(value.toGrpc))
      case value: MaxyConnectionValidationError =>
        GrpcDestinationError(GrpcDestinationError.OneOf.MaxyConnectionValidationError(value.toGrpc))
      case _ =>
        GrpcDestinationError(GrpcDestinationError.OneOf.Empty)
    }
  }

  implicit class DestinationErrorFromGrpc(lhs: GrpcDestinationError) {

    def fromGrpc: Either[GrpcConversionError, DestinationError] = lhs.oneOf match {
      case GrpcDestinationError.OneOf.Empty =>
        Left(GrpcConversionError("Unable to convert object from grpc type: GrpcDestinationError"))
      case GrpcDestinationError.OneOf.MaxyDestinationValidationError(value) =>
        value.fromGrpc
      case GrpcDestinationError.OneOf.MaxyConnectionValidationError(value) =>
        value.fromGrpc
    }
  }

  implicit class ConnectionErrorToGrpc(lhs: ConnectionError) {

    def toGrpc: GrpcConnectionError = lhs match {
      case value: MaxyConnectionValidationError =>
        GrpcConnectionError(GrpcConnectionError.OneOf.MaxyConnectionValidationError(value.toGrpc))
      case _ =>
        GrpcConnectionError(GrpcConnectionError.OneOf.Empty)
    }
  }

  implicit class ConnectionErrorFromGrpc(lhs: GrpcConnectionError) {

    def fromGrpc: Either[GrpcConversionError, ConnectionError] = lhs.oneOf match {
      case GrpcConnectionError.OneOf.Empty =>
        Left(GrpcConversionError("Unable to convert object from grpc type: GrpcConnectionError"))
      case GrpcConnectionError.OneOf.MaxyConnectionValidationError(value) =>
        value.fromGrpc
    }
  }

  implicit class FooToGrpc(lhs: Foo) {

    def toGrpc: GrpcFoo = {
      val _ = lhs
      GrpcFoo()
    }
  }

  implicit class FooFromGrpc(lhs: GrpcFoo) {

    def fromGrpc: Either[GrpcConversionError, Foo] = {
      val _ = lhs
      Right(Foo())
    }
  }

  implicit class TestToGrpc(lhs: Test) {

    def toGrpc: GrpcTest =
      GrpcTest(
        name = lhs.name,
        foo = lhs.foo.map(_.toGrpc),
        bar = lhs.bar,
        baz = lhs.baz.map(entry => (entry._1, entry._2.toGrpc)),
        opt0 = Some(lhs.opt0.toGrpc)
      )
  }

  implicit class TestFromGrpc(lhs: GrpcTest) {

    def fromGrpc: Either[GrpcConversionError, Test] =
      for {
        name <- Right(lhs.name.toList)
        foo <- lhs
          .foo
          .map(_.fromGrpc)
          .foldLeft(Right(Nil): Either[GrpcConversionError, List[Foo]])({
            case (acc, i) =>
              i.flatMap(a => acc.map(b => a :: b))
          })
        bar <- Right(lhs.bar)
        baz <- Right(
          lhs
            .baz
            .map(entry => (entry._1, entry._2.fromGrpc))
            .collect({
              case (a, Right(b)) =>
                (a, b)
            })
            .toMap
        )
        opt0 <- lhs.getOpt0.fromGrpc
      } yield Test(name = name, foo = foo, bar = bar, baz = baz, opt0 = opt0)
  }

  implicit class PersonToGrpc(lhs: Person) {

    def toGrpc: GrpcPerson =
      GrpcPerson(name = lhs.name, age = lhs.age, optOpt = Some(lhs.optOpt.toGrpc), opt3 = Some(lhs.opt3.toGrpc))
  }

  implicit class PersonFromGrpc(lhs: GrpcPerson) {

    def fromGrpc: Either[GrpcConversionError, Person] =
      for {
        name <- Right(lhs.name)
        age <- Right(lhs.age)
        optOpt <- lhs.getOptOpt.fromGrpc
        opt3 <- lhs.getOpt3.fromGrpc
      } yield Person(name = name, age = age, optOpt = optOpt, opt3 = opt3)
  }

  implicit class WatchToGrpc(lhs: Watch) {

    def toGrpc: GrpcWatch =
      GrpcWatch(
        time = lhs.time.toEpochSecond,
        alarms = lhs.alarms.map(toGrpcZonedDateTime),
        optionTime = Some(lhs.optionTime.toGrpc)
      )
  }

  implicit class WatchFromGrpc(lhs: GrpcWatch) {

    def fromGrpc: Either[GrpcConversionError, Watch] =
      for {
        time <- fromGrpcZonedDateTime(lhs.time)
        alarms <- lhs
          .alarms
          .map(fromGrpcZonedDateTime)
          .foldLeft(Right(Nil): Either[GrpcConversionError, List[ZonedDateTime]])({
            case (acc, i) =>
              i.flatMap(a => acc.map(b => a :: b))
          })
        optionTime <- lhs.getOptionTime.fromGrpc
      } yield Watch(time = time, alarms = alarms, optionTime = optionTime)
  }

  implicit class MaxyDestinationValidationErrorToGrpc(lhs: MaxyDestinationValidationError) {

    def toGrpc: GrpcMaxyDestinationValidationError =
      GrpcMaxyDestinationValidationError(
        code = lhs.code,
        maxyError = lhs.maxyError,
        person = Some(lhs.person.toGrpc),
        details = Some(lhs.details.toGrpc)
      )
  }

  implicit class MaxyDestinationValidationErrorFromGrpc(lhs: GrpcMaxyDestinationValidationError) {

    def fromGrpc: Either[GrpcConversionError, MaxyDestinationValidationError] =
      for {
        code <- Right(lhs.code)
        maxyError <- Right(lhs.maxyError)
        person <- lhs.getPerson.fromGrpc
        details <- lhs.getDetails.fromGrpc
      } yield MaxyDestinationValidationError(code = code, maxyError = maxyError, person = person, details = details)
  }

  implicit class MaxyConnectionValidationErrorToGrpc(lhs: MaxyConnectionValidationError) {

    def toGrpc: GrpcMaxyConnectionValidationError =
      GrpcMaxyConnectionValidationError(code = lhs.code, maxyError = lhs.maxyError, person = Some(lhs.person.toGrpc))
  }

  implicit class MaxyConnectionValidationErrorFromGrpc(lhs: GrpcMaxyConnectionValidationError) {

    def fromGrpc: Either[GrpcConversionError, MaxyConnectionValidationError] =
      for {
        code <- Right(lhs.code)
        maxyError <- Right(lhs.maxyError)
        person <- lhs.getPerson.fromGrpc
      } yield MaxyConnectionValidationError(code = code, maxyError = maxyError, person = person)
  }

  implicit class MyTraitToGrpc(lhs: MyTrait) {

    def toGrpc: GrpcMyTrait = lhs match {
      case value: MyClass =>
        GrpcMyTrait(GrpcMyTrait.OneOf.MyClass(value.toGrpc))
      case _ =>
        GrpcMyTrait(GrpcMyTrait.OneOf.Empty)
    }
  }

  implicit class MyTraitFromGrpc(lhs: GrpcMyTrait) {

    def fromGrpc: Either[GrpcConversionError, MyTrait] = lhs.oneOf match {
      case GrpcMyTrait.OneOf.Empty =>
        Left(GrpcConversionError("Unable to convert object from grpc type: GrpcMyTrait"))
      case GrpcMyTrait.OneOf.MyClass(value) =>
        value.fromGrpc
    }
  }

  implicit class MyClassToGrpc(lhs: MyClass) {

    def toGrpc: GrpcMyClass = {
      val _ = lhs
      GrpcMyClass()
    }
  }

  implicit class MyClassFromGrpc(lhs: GrpcMyClass) {

    def fromGrpc: Either[GrpcConversionError, MyClass] = {
      val _ = lhs
      Right(MyClass())
    }
  }

  implicit class OptionToGrpc(lhs: Option[List[String]]) {

    def toGrpc: GrpcMaybeListString =
      lhs match {
        case None =>
          GrpcMaybeListString(GrpcMaybeListString.OneOf.Nonne(GrpcNonne()))
        case Some(value) =>
          GrpcMaybeListString(GrpcMaybeListString.OneOf.Somme(GrpcListString(value)))
      }
  }

  implicit class OptionFromGrpc(lhs: GrpcMaybeListString) {

    def fromGrpc: Either[GrpcConversionError, Option[List[String]]] = lhs.oneOf match {
      case GrpcMaybeListString.OneOf.Somme(value) =>
        Right(Some(value.list.toList))
      case _ =>
        Right(None)
    }
  }

  implicit class OptionStringToGrpc(lhs: Option[String]) {

    def toGrpc: GrpcMaybeString =
      lhs match {
        case None =>
          GrpcMaybeString(GrpcMaybeString.OneOf.Nonne(GrpcNonne()))
        case Some(value) =>
          GrpcMaybeString(GrpcMaybeString.OneOf.Somme(value))
      }
  }

  implicit class OptionStringFromGrpc(lhs: GrpcMaybeString) {

    def fromGrpc: Either[GrpcConversionError, Option[String]] = lhs.oneOf match {
      case GrpcMaybeString.OneOf.Somme(value) =>
        Right(Some(value))
      case _ =>
        Right(None)
    }
  }

  implicit class OptionOptionStringToGrpc(lhs: Option[Option[String]]) {

    def toGrpc: GrpcMaybeMaybeString =
      lhs match {
        case None =>
          GrpcMaybeMaybeString(GrpcMaybeMaybeString.OneOf.Nonne(GrpcNonne()))
        case Some(value) =>
          GrpcMaybeMaybeString(GrpcMaybeMaybeString.OneOf.Somme(value.toGrpc))
      }
  }

  implicit class OptionOptionStringFromGrpc(lhs: GrpcMaybeMaybeString) {

    def fromGrpc: Either[GrpcConversionError, Option[Option[String]]] = lhs.oneOf match {
      case GrpcMaybeMaybeString.OneOf.Somme(value) =>
        value.fromGrpc.map(Some(_))
      case _ =>
        Right(None)
    }
  }

  implicit class OptionTestToGrpc(lhs: Option[Test]) {

    def toGrpc: GrpcMaybeTest =
      lhs match {
        case None =>
          GrpcMaybeTest(GrpcMaybeTest.OneOf.Nonne(GrpcNonne()))
        case Some(value) =>
          GrpcMaybeTest(GrpcMaybeTest.OneOf.Somme(value.toGrpc))
      }
  }

  implicit class OptionTestFromGrpc(lhs: GrpcMaybeTest) {

    def fromGrpc: Either[GrpcConversionError, Option[Test]] = lhs.oneOf match {
      case GrpcMaybeTest.OneOf.Somme(value) =>
        value.fromGrpc.map(Some(_))
      case _ =>
        Right(None)
    }
  }

  implicit class OptionZonedDateTimeToGrpc(lhs: Option[ZonedDateTime]) {

    def toGrpc: GrpcMaybeZonedDateTime =
      lhs match {
        case None =>
          GrpcMaybeZonedDateTime(GrpcMaybeZonedDateTime.OneOf.Nonne(GrpcNonne()))
        case Some(value) =>
          GrpcMaybeZonedDateTime(GrpcMaybeZonedDateTime.OneOf.Somme(toGrpcZonedDateTime(value)))
      }
  }

  implicit class OptionZonedDateTimeFromGrpc(lhs: GrpcMaybeZonedDateTime) {

    def fromGrpc: Either[GrpcConversionError, Option[ZonedDateTime]] = lhs.oneOf match {
      case GrpcMaybeZonedDateTime.OneOf.Somme(value) =>
        fromGrpcZonedDateTime(value).map(Some(_))
      case _ =>
        Right(None)
    }
  }

}
