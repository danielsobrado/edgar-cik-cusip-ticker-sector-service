# Edgar CIK Downloader
This project is a Spring Boot-based service that downloads and updates CIK (Central Index Key) data from the SEC (U.S. Securities and Exchange Commission) for public companies. 

The service stores the CIK data in a RDBMS (e.g. MySQL) database and supports enrichment with stock exchange information.

There are endpoints to retrieve the CIK data by CIK or ticker.

A cron expression is used to schedule the CIK data update process.

![Diagram](https://github.com/danielsobrado/edgar-cik-ticker-service/blob/ca007c90c25fa370765b3b4b624296a6fb534a87/doc/images/Diagram.PNG)

## Features
* Download CIK data from the SEC and store it in a MySQL database.
* 2 sources of CIK data, one with stock exchange information.
* Scheduled CIK data updates using cron expressions.
* Configurable properties for URLs and scheduling.
* Retrieve stock information by CIK or ticker through RESTful API endpoints.
* Monitor and log CIK data updates and process executions.
* Utilize OpenAPI/Swagger for API documentation and testing.

## Requirements
* Java 17
* MySQL 8

## Configuration
All the configuration settings can be found in the `src/main/resources/application.yml` file. You can change the following properties:

* `edgar.company-tickers-url`: URL for downloading the company tickers JSON data.
  * Default value: `https://www.sec.gov/files/company_tickers.json`
* `edgar.company-tickers-exchange-url`: URL for downloading the company tickers with exchange JSON data.
  * Default value: `https://www.sec.gov/files/company_tickers_exchange.json`
* `edgar.enrich-sector-url`: URL for enriching the sector data based on the CIK value.
  * Default value: `https://www.sec.gov/cgi-bin/browse-edgar?CIK={cik}`
* `edgar.cik-update-cron`: Cron expression for scheduling CIK data updates.
  * Default value: `0 0 0 1 * ?`
* `edgar.cik-exchange-update-cron`: Cron expression for scheduling CIK data updates with exchange information.
  * Default value: `0 */20 * * * *`
* `edgar.unenriched-cron`: Cron expression for scheduling the enrichment of unenriched CIKs.
  * Default value: `0 0 0 * * *`
* `edgar.sector-enrich-cron`: Cron expression for scheduling the enrichment of sector data.
  * Default value: `*/10 * * * * *`
* `edgar.use-tickers`: Enable or disable the company tickers downloader.
  * Default value: `false`
* `edgar.use-tickers-exchange`: Enable or disable the company tickers with exchange downloader.
  * Default value: `true`
* `edgar.use-sector-enrich`: Enable or disable the sector enrichment feature.
  * Default value: `true`

You can modify these properties according to your requirements. For example, you may change the cron expressions to adjust the frequency of scheduled tasks or enable/disable certain features of the application.

## Database

Install MySQL and create a Database:

```SQL
CREATE DATABASE edgar;

CREATE USER 'edgar_user'@'%' IDENTIFIED BY 'password';

GRANT ALL PRIVILEGES ON edgar.* TO 'edgar_user'@'%';

FLUSH PRIVILEGES;
```

Or use the following for Postgres:

```SQL
CREATE DATABASE edgar;

CREATE USER edgar_user WITH ENCRYPTED PASSWORD 'password';

GRANT ALL PRIVILEGES ON DATABASE edgar TO edgar_user;

ALTER USER edgar_user CREATEDB;
```

And the following connection string in your `application.properties`.

`jdbc:postgresql://localhost:5432/edgar`

After an initial execution you can see that the main table gets populated:

![MySQL](https://github.com/danielsobrado/edgar-cik-ticker-service/blob/692b99d2d86680d1e86ea77ad3557d6cd33474f1/doc/images/FromMySQLWorkbench.PNG)

## Getting Started
1. Clone the repository:

```bash
git clone https://github.com/danielsobrado/edgar-cik-ticker-service.git
cd edgar-cik-ticker-service
```

2. Update the src/main/resources/application.yml file with your MySQL connection information.

3. Build the project:

```bash
./gradlew build
```

4. Run the project:

```bash
./gradlew bootRun
```

The service will start and automatically download and update CIK data based on the specified cron expressions in the application.yml file.

## Endpoints
* GET `/api/stocks/cik/{cik}`: Retrieves the stock information by CIK.
* GET `/api/stocks/ticker/{ticker}`: Retrieves the stock information by ticker. 
* GET `/api/stocks/sector/{sector}`: Retrieves a list of stock information by sector. 
* GET `/api/stocks/sic/{sic}`: Retrieves a list of stock information by SIC code (tag).
  All endpoints return a JSON object or a list of JSON objects with the stock information if found or a 404 status code if the CIK, ticker, sector, or SIC code is not found.

Example of end point use:

![Endpoint](https://github.com/danielsobrado/edgar-cik-ticker-service/blob/692b99d2d86680d1e86ea77ad3557d6cd33474f1/doc/images/APIExample.PNG)

## Process Execution Tracking
This application keeps track of the last execution time of the CIK data update process. The purpose of this tracking is to ensure that the process is executed immediately if the last execution date is more than one month ago, or if the tracking table is empty (e.g., the application is run for the first time).

## Enrichment
A CIK can be enhanced utilizing a REST API endpoint using another service that scrapes Edgar to provide further data. The maximum rate for this service will be one inquiry per ten seconds.
We'll have a scheduled procedure that will check the database for entries that haven't been enriched and enrich them one at a time using a cron job that can be configured.
Using configuration we can determine whether to enable this service or not.

The number of unenriched records in the system will be counted by another scheduled activity, which will run once every day. The previous scrapper won't need to be run if there are no records to be enriched.

![Diagram 2](https://github.com/danielsobrado/edgar-cik-ticker-service/blob/22dffc0865942e39cce197e3ce53a1981631710f/doc/images/Diagram2.PNG)

## Notes
* The CIK data is updated every month, so the cron expression for the CIK data update process should be set to run once a month.
* There are some cases where the CIK is duplicated in the CIK data file. In these cases, the service will use the first CIK found for the ticker. 
See:

![Logs](https://github.com/danielsobrado/edgar-cik-ticker-service/blob/692b99d2d86680d1e86ea77ad3557d6cd33474f1/doc/images/LogsEdgarUpdate.PNG)

Disclaimer: I used chatGPT extensively for this project.

Please feel free to contact me if you have any questions or suggestions.

![Daniel Sobrado](https://github.com/danielsobrado/edgar-cik-ticker-service/blob/692b99d2d86680d1e86ea77ad3557d6cd33474f1/doc/images/Signed.PNG)