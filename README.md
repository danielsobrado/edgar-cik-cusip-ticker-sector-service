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

Note: The performance of batch updates is much better, MySQL has issues with identity IDs for batch and Postgres does not.

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

## API Endpoints

### Stock Information

- `GET /api/v1/stocks/cik/{cik}`: Retrieves the stock information by CIK.
- `GET /api/v1/stocks/ticker/{ticker}`: Retrieves the stock information by ticker.
- `GET /api/v1/stocks?sector={sector}`: Retrieves a list of stock information by sector.
- `GET /api/v1/stocks?sic={sic}`: Retrieves a list of stock information by SIC code (tag).

### Enrich Stock Information

- `POST /api/v1/stocks/enrich/ticker/{ticker}`: Enriches the stock information by ticker.
- `GET /api/v1/stocks/enrich/cusip`: Enriches the stock information with CUSIP extracted from the Edgar filings.
- `GET /api/v1/stocks/enrich/cusip/from-filings`: Generates a mapping file based on the given filing types.

### Export Stock Information

- `GET /api/v1/stocks/export/csv`: Exports the CIK data to a CSV file.

### Filings

- `GET /api/v1/stocks/filings/{filingType}`: Downloads filings of the given filing type.

### Form Types

- `GET /api/v1/stocks/formTypes`: Retrieves a list of distinct form types from the full index.

### Full Index

- `POST /api/v1/stocks/fullIndex`: Initiates the download of the full index.
- `GET /api/v1/edgar/download_full_index?year={year}&quarter={quarter}`: Downloads and processes the full index for a specific year and quarter.

Example of end point use:

![Endpoint](https://github.com/danielsobrado/edgar-cik-ticker-service/blob/692b99d2d86680d1e86ea77ad3557d6cd33474f1/doc/images/APIExample.PNG)

## Download Full Index for a Specific Year and Quarter

This endpoint downloads and processes the EDGAR master index file for the specified year and quarter.

### Request

`GET /api/v1/edgar/download_full_index`

| Parameter | Type   | Description                                      |
|-----------|--------|--------------------------------------------------|
| year      | int    | The year of the index file to download.          |
| quarter   | int    | The quarter of the index file to download (1-4). |

### Response

The endpoint returns an HTTP 204 No Content status on successful processing, or an appropriate error status with a message if there's an issue.

#### Successful Response

- Status: `204 No Content`

#### Error Response

- Status: `400 Bad Request`
  - Description: Invalid input parameters.
  - Example: `{"message": "Invalid year or quarter."}`

- Status: `500 Internal Server Error`
  - Description: An error occurred during processing.
  - Example: `{"message": "Failed to download and process the master index file."}`

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