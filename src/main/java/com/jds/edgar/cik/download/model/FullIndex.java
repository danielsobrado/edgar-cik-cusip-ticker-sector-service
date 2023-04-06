package com.jds.edgar.cik.download.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "full_index")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FullIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private Long cik;
    private String companyName;
    private String formType;
    private String dateFiled;
    private String filename;
}
