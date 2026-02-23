package com.cashier.service;

import com.cashier.model.Product;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 条码查询服务
 * 支持多个免费的条码查询 API
 */
public class BarcodeQueryService {
    private static final Logger logger = LoggerFactoryUtil.getLogger(BarcodeQueryService.class);
    
    // API 配置
    private static final String JUHE_API_URL = "http://apis.juhe.cn/cnbarcode/query?key=";
    private static final String TIANAPI_API_URL = "http://api.tianapi.com/tianapi/tybarcode/index?key=";
    
    // API 密钥（需要从配置文件或数据库读取）
    private String juheApiKey = "";
    private String tianapiKey = "";
    
    // 超时时间（毫秒）
    private static final int TIMEOUT = 5000;
    
    /**
     * 构造函数
     */
    public BarcodeQueryService() {
        // 从系统设置中读取 API 密钥
        loadApiKeys();
    }
    
    /**
     * 从系统设置加载 API 密钥
     */
    private void loadApiKeys() {
        try {
            // TODO: 从数据库或配置文件读取 API 密钥
            // 这里使用占位符，实际需要从 SystemSettingsDAO 读取
            juheApiKey = getSetting("juhe_api_key", "");
            tianapiKey = getSetting("tianapi_key", "");
            
            if (juheApiKey.isEmpty() && tianapiKey.isEmpty()) {
                logger.warn("No API keys configured for barcode query");
            }
        } catch (Exception e) {
            logger.error("Failed to load API keys", e);
        }
    }
    
    /**
     * 从设置中获取值
     */
    private String getSetting(String key, String defaultValue) {
        // TODO: 实现从数据库获取设置的逻辑
        return defaultValue;
    }
    
    /**
     * 查询条码信息
     * 支持多个 API，按顺序尝试
     * 
     * @param barcode 条码
     * @return 商品信息，如果查询失败返回 null
     */
    public Product queryBarcode(String barcode) {
        if (barcode == null || barcode.isEmpty()) {
            logger.warn("Barcode is empty");
            return null;
        }
        
        logger.info("Querying barcode: {}", barcode);
        
        List<Product> results = new ArrayList<>();
        
        // 尝试使用聚合数据 API
        if (!juheApiKey.isEmpty()) {
            try {
                Product product = queryByJuhe(barcode);
                if (product != null) {
                    results.add(product);
                }
            } catch (Exception e) {
                logger.warn("Juhe API query failed for barcode {}", barcode, e);
            }
        }
        
        // 尝试使用天聚数据 API
        if (!tianapiKey.isEmpty()) {
            try {
                Product product = queryByTianapi(barcode);
                if (product != null) {
                    results.add(product);
                }
            } catch (Exception e) {
                logger.warn("Tianapi query failed for barcode {}", barcode, e);
            }
        }
        
        // 返回第一个成功的结果
        if (!results.isEmpty()) {
            Product product = results.get(0);
            logger.info("Found product for barcode {}: {}", barcode, product.getName());
            return product;
        }
        
        logger.warn("No product found for barcode: {}", barcode);
        return null;
    }
    
    /**
     * 使用聚合数据 API 查询
     */
    private Product queryByJuhe(String barcode) throws Exception {
        String urlStr = JUHE_API_URL + juheApiKey + "&barcode=" + URLEncoder.encode(barcode, StandardCharsets.UTF_8);
        
        String response = httpGet(urlStr);
        
        // 解析 JSON 响应
        // 聚合数据 API 响应格式: {"error_code":0,"reason":"Success","result":{...}}
        // TODO: 实现 JSON 解析
        
        logger.debug("Juhe API response: {}", response);
        
        // 临时返回 null，实际需要解析响应
        return parseJuheResponse(response);
    }
    
    /**
     * 使用天聚数据 API 查询
     */
    private Product queryByTianapi(String barcode) throws Exception {
        String urlStr = TIANAPI_API_URL + tianapiKey + "&barcode=" + URLEncoder.encode(barcode, StandardCharsets.UTF_8);
        
        String response = httpGet(urlStr);
        
        // 解析 JSON 响应
        // 天聚数据 API 响应格式: {"code":200,"msg":"success","result":{...}}
        // TODO: 实现 JSON 解析
        
        logger.debug("Tianapi response: {}", response);
        
        // 临时返回 null，实际需要解析响应
        return parseTianapiResponse(response);
    }
    
    /**
     * HTTP GET 请求
     */
    private String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(TIMEOUT);
        conn.setReadTimeout(TIMEOUT);
        
        conn.connect();
        
        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("HTTP error: " + responseCode);
        }
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } finally {
            conn.disconnect();
        }
    }
    
    /**
     * 解析聚合数据 API 响应
     */
    private Product parseJuheResponse(String jsonResponse) {
        // TODO: 实现 JSON 解析
        // 示例响应:
        // {"error_code":0,"reason":"Success","result":{"barcode":"6901234567890","name":"商品名称","spec":"规格","unit":"单位","price":10.0}}
        
        return null;
    }
    
    /**
     * 解析天聚数据 API 响应
     */
    private Product parseTianapiResponse(String jsonResponse) {
        // TODO: 实现 JSON 解析
        // 示例响应:
        // {"code":200,"msg":"success","result":{"barcode":"6901234567890","name":"商品名称","spec":"规格","unit":"单位","price":10.0}}
        
        return null;
    }
    
    /**
     * 设置 API 密钥
     */
    public void setApiKeys(String juheKey, String tianapiKey) {
        this.juheApiKey = juheKey;
        this.tianapiKey = tianapiKey;
        
        // 保存到数据库
        // TODO: 保存 API 密钥到系统设置
    }
    
    /**
     * 批量查询条码
     */
    public List<Product> queryBarcodes(List<String> barcodes) {
        List<Product> results = new ArrayList<>();
        
        for (String barcode : barcodes) {
            try {
                Product product = queryBarcode(barcode);
                if (product != null) {
                    results.add(product);
                }
            } catch (Exception e) {
                logger.error("Failed to query barcode: {}", barcode, e);
            }
        }
        
        return results;
    }
    
    /**
     * 检查 API 是否可用
     */
    public boolean isApiAvailable() {
        return !juheApiKey.isEmpty() || !tianapiKey.isEmpty();
    }
}