package interview.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据库连接测试控制器
 * 用于验证数据库连接和检查表结构
 */
@RestController
@RequestMapping("/api/database")
public class DatabaseTestController {

    @Autowired
    private DataSource dataSource;

    /**
     * 测试数据库连接
     */
    @GetMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> result = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            result.put("success", true);
            result.put("message", "数据库连接成功");
            result.put("databaseURL", metaData.getURL());
            result.put("databaseName", connection.getCatalog());
            result.put("userName", metaData.getUserName());
            result.put("driverName", metaData.getDriverName());
            result.put("driverVersion", metaData.getDriverVersion());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "数据库连接失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.ok(result);
        }
    }

    /**
     * 检查users表是否存在及结构
     */
    @GetMapping("/check-users-table")
    public ResponseEntity<Map<String, Object>> checkUsersTable() {
        Map<String, Object> result = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            // 检查users表是否存在
            ResultSet tables = metaData.getTables(null, null, "users", null);
            boolean tableExists = tables.next();
            
            result.put("tableExists", tableExists);
            
            if (tableExists) {
                // 获取表的列信息
                ResultSet columns = metaData.getColumns(null, null, "users", null);
                Map<String, String> columnInfo = new HashMap<>();
                
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    String columnType = columns.getString("TYPE_NAME");
                    String isNullable = columns.getString("IS_NULLABLE");
                    columnInfo.put(columnName, columnType + " (nullable: " + isNullable + ")");
                }
                
                result.put("columns", columnInfo);
                result.put("message", "users表存在，结构已获取");
            } else {
                result.put("message", "users表不存在");
            }
            
            result.put("success", true);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "检查表结构失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.ok(result);
        }
    }

    /**
     * 查询users表中的用户数量
     */
    @GetMapping("/count-users")
    public ResponseEntity<Map<String, Object>> countUsers() {
        Map<String, Object> result = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            var statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT COUNT(*) as user_count FROM users");
            
            if (rs.next()) {
                int userCount = rs.getInt("user_count");
                result.put("success", true);
                result.put("userCount", userCount);
                result.put("message", "成功查询用户数量");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询用户数量失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.ok(result);
        }
    }
} 