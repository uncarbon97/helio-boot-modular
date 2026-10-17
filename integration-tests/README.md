# 集成测试
## 运行命令
```
-Dskip-unit-tests=true verify -f pom.xml
```

## Maven 配置文件
- run-it

## 环境变量
```
DB_HOST=;DB_PORT=;DB_NAME=;DB_USERNAME=;DB_PASSWORD=;REDIS_HOST=;JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"
```

# 单元测试+输出JaCoCo覆盖率报告
## 运行命令
```
-Dskip-unit-tests=false verify -f pom.xml
```

