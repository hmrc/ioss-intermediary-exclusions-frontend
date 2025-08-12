
# ioss-intermediary-exclusions-frontend

This is the repository for Import One Stop Shop Intermediary Exclusions Frontend

Backend: https://github.com/hmrc/ioss-intermediary-registration

Stub: https://github.com/hmrc/ioss-intermediary-registration-stub

Exclusions Service
------------
The main function of this service is to allow traders who are registered with the Import One Stop Shop Intermediary Registration
service to leave the Import One Stop Shop service, if they are no longer eligible to use it.


Requirements
------------

This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), so needs at least a [JRE] to run.

## Run the application locally via Service Manager

```
sm2 --start IMPORT_ONE_STOP_SHOP_ALL
```

### To run the application locally from the repository, execute the following:

```
sm2 --stop IOSS_INTERMEDIARY_EXCLUSIONS_FRONTEND
```
and
```
sbt run
```

### Running correct version of mongo
Mongo 6 with a replica set is required to run the service. Please refer to the MDTP Handbook for instructions on how to run this

### Using the application

Access the Authority Wizard to log in:
http://localhost:9949/auth-login-stub/gg-sign-in

Enter the following details on this page and submit:
- Redirect URL: http://localhost:10177/pay-clients-vat-on-eu-sales/leave-import-one-stop-shop-intermediary
- Affinity Group: Organisation
- Enrolments (there are two rows this time):
- Enrolment Key: HMRC-MTD-VAT
- Identifier Name: VRN
- Identifier Value: 100000001
- Enrolment Key: HMRC-IOSS-INT
- Identifier Name: IntNumber
- Identifier Value: IN9001234567

It is recommended to use VRN 100000001 and Int Number IN9001234567 for a regular exclusions journey, however
alternatives can be found in the ioss-intermediary-registration-stub which holds scenarios for registered traders and any
existing exclusions.

Unit and Integration Tests
------------

To run the unit and integration tests, you will need to open an sbt session in the terminal.

### Unit Tests

To run all tests, run the following command in your sbt session:
```
test
```

To run a single test, run the following command in your sbt session:
```
testOnly <package>.<SpecName>
```

An asterisk can be used as a wildcard character without having to enter the package, as per the example below:
```
testOnly *MoveCountryControllerSpec
```

### Integration Tests

To run all tests, run the following command in your sbt session:
```
it:test
```

To run a single test, run the following command in your sbt session:
```
it:testOnly <package>.<SpecName>
```

An asterisk can be used as a wildcard character without having to enter the package, as per the example below:
```
it:testOnly *SessionRepositorySpec
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").