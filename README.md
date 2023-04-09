# Sec's Edgar Ticker downloader and enricher with CIK, CUSIP and SIC mappings
This project is a Spring Boot-based service that downloads and updates CIK (Central Index Key) data from the SEC (U.S. Securities and Exchange Commission) for public companies. 

The service stores the CIK data in a RDBMS (e.g. MySQL) database and supports enrichment with stock exchange information.

There are endpoints to retrieve the CIK data by CIK or ticker.

### CIK (Central Index Key)

CIK is a unique identifier assigned by the U.S. Securities and Exchange Commission (SEC) to companies and individuals who are required to file disclosures with the SEC. It is used to track and access filings and forms submitted by these entities.

#### Usage in SEC Filings

The CIK is used in the following ways:

1. To uniquely identify companies and individuals required to submit filings to the SEC.
2. To search for and access filings and forms submitted by these entities in the SEC's EDGAR (Electronic Data Gathering, Analysis, and Retrieval) database.

### CUSIP (Committee on Uniform Securities Identification Procedures)

CUSIP is a unique identifier for financial instruments, such as stocks and bonds, issued in the United States and Canada. The CUSIP system is owned by the American Bankers Association (ABA) and is operated by S&P Global Market Intelligence.

A CUSIP consists of a nine-character alphanumeric code, where the first six characters represent the issuer, the next two characters indicate the issue, and the last character is a check digit.

#### Usage in SEC Filings

CUSIP is used in the following ways:

1. To uniquely identify financial instruments, such as stocks and bonds, in the context of trading, clearance, and settlement.
2. To facilitate the processing and communication of financial transactions, ensuring accurate tracking and reporting.


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
* Download filings of specific form types or the ones containing '13' in their form type, each stored in its respective folder.
* Enrich stock information by extracting additional data such as SIC and sector.
* Retry failed enrichment attempts up to 3 times with a 5-second interval between each attempt.
* Flag records with errors and retry only once in the next run for these records.
* Enrich stock information with CUSIP extracted from the Edgar filings.
* Generate a mapping file based on the given filing types.


## Requirements
* Java 17
* MySQL 8 or Postgres
* Optional: Docker

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

To build and start a database container, you can use the following commands from `/scripts/postgres`:

```bash
docker build -t edgar-postgres .
docker volume create edgar-postgres-data
docker run -d --name edgar-postgres -p 5432:5432 -v edgar-postgres-data:/var/lib/postgresql/data postgres:latest
```

Or the equivalent for MySQL:

```bash
docker build -t edgar-mysql .
docker volume create edgar-mysql-data
docker run -d --name edgar-mysql -p 3306:3306 -v edgar-mysql-data:/var/lib/mysql mysql:latest
```

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

There are other fields that can be enriched from the filings:

![Enrich from filings](https://github.com/danielsobrado/edgar-cik-cusip-ticker-sector-service/blob/61c58da7789820f71ebe5fa0dc0a09525bea9fb5/doc/images/Filing13GExample.png)

### Error Cases for Enriching CIK

The `enrichCik` method handles various scenarios when enriching a CIK. The following are the possible error cases and how they are managed:

1. **No matching Ticker Symbol**: When the provided ticker does not match any CIK, the method will return an `EnrichedData` object with the following values:

  - `sic`: "Not Found"
  - `sector`: "Not Found"

2. **No matching CIK**: When the provided CIK does not have a matching record, the method will return an `EnrichedData` object with the following values:

  - `sic`: "No CIK"
  - `sector`: "No CIK"

3. **Sector not available**: When the sector cannot be extracted from the page, the method will return an `EnrichedData` object with the following values:

  - `sic`: (extracted SIC value)
  - `sector`: "Not Available"

4. **Error during enrichment**: When an error occurs during the enrichment process, such as an IOException, the method will retry the operation up to three times (with a 5-second interval between attempts). If the enrichment still fails after the retries, it will mark the record with an error message and save it to the repository. For records flagged with errors, the method will only retry once in the next run.

  - Add a `lastError` field to the `Stock` class to store the error message from the last failed attempt. Update the `Stock` class with the necessary getter and setter methods for this field.

## Notes
* The CIK data is updated every month, so the cron expression for the CIK data update process should be set to run once a month.
* There are some cases where the CIK is duplicated in the CIK data file. In these cases, the service will use the first CIK found for the ticker. 
See:

![Logs](https://github.com/danielsobrado/edgar-cik-ticker-service/blob/692b99d2d86680d1e86ea77ad3557d6cd33474f1/doc/images/LogsEdgarUpdate.PNG)

## Final notes

As the form isn't submitted until 45 days following the end of the quarter, a lot might change regarding investments during that time. It's also possible that by this point, the projected price changes have already taken place.

Even with holdings information, it might be challenging to determine an asset manager's net position. A fund may be net long, net short, or more likely have a limited pay-off if it discloses holding long positions in the shares, put options, and call options. Moreover, there may be unlisted positions that call for longs in the underlying security while the manager retains a net short exposure or an investor may be partially hedged, such as short positions in convertible bonds.

Please feel free to contact me if you have any questions or suggestions.

![Daniel Sobrado](https://github.com/danielsobrado/edgar-cik-ticker-service/blob/692b99d2d86680d1e86ea77ad3557d6cd33474f1/doc/images/Signed.PNG)
