# Edgar CIK Downloader
This project is a Spring Boot-based service that downloads and updates CIK (Central Index Key) data from the SEC (U.S. Securities and Exchange Commission) for public companies. The service stores the CIK data in a MySQL database and supports enrichment with stock exchange information.

## Features
* Download CIK data from the SEC and store it in a MySQL database.
* Update CIK data with stock exchange information.
* Scheduled CIK data updates using cron expressions.
* Configurable properties for URLs and scheduling.

## Requirements
* Java 17
* MySQL 8

## Configuration
Configuration
All the configuration settings can be found in the src/main/resources/application.yml file. You can change the following properties:

* `edgar.company-tickers-url`: URL for downloading the company tickers JSON data.
* `edgar.company-tickers-exchange-url`: URL for downloading the company tickers with exchange JSON data.
* `edgar.cik-update-cron`: Cron expression for scheduling CIK data updates.
* `edgar.cik-exchange-update-cron`: Cron expression for scheduling CIK data updates with exchange information.
* `edgar.use-tickers`: Enable or disable this downloader
* `edgar.use-tickers-exchange`: Enable or disable this downloader

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
* GET /api/stocks/cik/{cik}: Retrieves the stock information by CIK.
* GET /api/stocks/ticker/{ticker}: Retrieves the stock information by ticker.
Both endpoints return a JSON object with the stock information if found, or a 404 status code if the CIK or ticker is not found.

Example of end point use:

![Endpoint](https://github.com/danielsobrado/edgar-cik-ticker-service/blob/692b99d2d86680d1e86ea77ad3557d6cd33474f1/doc/images/APIExample.PNG)

## Process Execution Tracking
This application keeps track of the last execution time of the CIK data update process. The purpose of this tracking is to ensure that the process is executed immediately if the last execution date is more than one month ago, or if the tracking table is empty (e.g., the application is run for the first time).

## Notes
* The CIK data is updated every month, so the cron expression for the CIK data update process should be set to run once a month.
* There are some cases where the CIK is duplicated in the CIK data file. In these cases, the service will use the first CIK found for the ticker. 
See:

![Logs](https://github.com/danielsobrado/edgar-cik-ticker-service/blob/692b99d2d86680d1e86ea77ad3557d6cd33474f1/doc/images/LogsEdgarUpdate.PNG)

Please feel free to contact me if you have any questions or suggestions.

![Daniel Sobrado](https://github.com/danielsobrado/edgar-cik-ticker-service/blob/692b99d2d86680d1e86ea77ad3557d6cd33474f1/doc/images/Signed.PNG)