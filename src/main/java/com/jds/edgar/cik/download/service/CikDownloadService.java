package com.jds.edgar.cik.download.service;

public interface CikDownloadService {
    //    @Scheduled(cron = "${edgar.cik-update-cron}")
    void downloadCikData();
}
