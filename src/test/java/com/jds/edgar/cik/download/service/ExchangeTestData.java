package com.jds.edgar.cik.download.service;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ExchangeTestData {
    private List<String> fields;
    private List<List<Object>> data;
}
