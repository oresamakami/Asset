package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "assets")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 資産種別 (PC / 携帯) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetType assetType;

    /** 管理用QRコードID */
    @Column(nullable = false, unique = true)
    private String qrCodeId;

    /** 品名 */
    @Column(nullable = false)
    private String productName;

    /** 型名 */
    private String modelName;

    /** 製造番号 (Serial) */
    @Column(unique = true)
    private String serialNumber;

    /** OS */
    private String os;

    /** CPU */
    @Column(name = "`cpu`")
    private String cpu;

    /** メモリ */
    @Column(name = "`memory`")
    private String memory;

    /** ストレージ */
    @Column(name = "`storage`")
    private String storage;

    /** スペック (その他) */
    @Column(length = 1000)
    private String spec;

    /** 購入日 */
    private LocalDate purchaseDate;

    /** ステータス */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetStatus status;

    /** 製品画像パス */
    private String imagePath;
}
