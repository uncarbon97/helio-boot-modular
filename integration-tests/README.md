# 只跑集成测试
## 运行命令
```
-Dskip-unit-tests=true -Prun-it   verify -f pom.xml
```

## 环境变量
```
DB_HOST=;DB_PORT=;DB_NAME=;DB_USERNAME=;DB_PASSWORD=;REDIS_HOST=;JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"
```

# 只跑单元测试
## 运行命令
```
-Dskip-unit-tests=false verify -f pom.xml
```

# 全量测试
## 运行命令
```
-Dskip-unit-tests=false -Prun-it   verify -f pom.xml
```

覆盖率由 jacoco-aggregate 自动合并两份 exec。
