package com.cashier.model;

import java.sql.Timestamp;

/**
 * 供应商模型
 */
public class Supplier {
    public int id;               // 供应商ID（数据库自增主键）
    public String supplierCode;  // 供应商编号
    public String name;          // 供应商名称
    public String contactPerson; // 联系人
    public String phone;         // 联系电话
    public String address;       // 地址
    public String rank;          // 供应商分级（A级、B级、C级）
    public boolean status;       // 状态（true-启用，false-禁用）
    public String remark;        // 备注
    public Timestamp createTime; // 创建时间
    public Timestamp updateTime; // 更新时间

    public Supplier() {
        this.id = 0;
        this.supplierCode = "";
        this.name = "";
        this.contactPerson = "";
        this.phone = "";
        this.address = "";
        this.rank = "C";
        this.status = true;
        this.remark = "";
        this.createTime = new Timestamp(System.currentTimeMillis());
        this.updateTime = new Timestamp(System.currentTimeMillis());
    }

    public Supplier(String name, String contactPerson, String phone) {
        this();
        this.name = name;
        this.contactPerson = contactPerson;
        this.phone = phone;
    }

    /**
     * 获取等级显示名称
     */
    public String getRankDisplayName() {
        switch (rank) {
            case "A":
                return "A级";
            case "B":
                return "B级";
            case "C":
                return "C级";
            default:
                return rank;
        }
    }

    /**
     * 获取状态显示名称
     */
    public String getStatusDisplayName() {
        return status ? "启用" : "禁用";
    }
}