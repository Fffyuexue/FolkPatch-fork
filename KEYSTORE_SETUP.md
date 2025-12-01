# 密钥管理说明

## 安全警告
为了保护您的应用签名安全，请勿将真实的密钥文件和配置提交到Git仓库。

## 配置步骤
1. 创建您自己的密钥库文件（.jks）
2. 将 `keystore.properties.template` 复制为 `keystore.properties`
3. 在 `keystore.properties` 中填入您的实际密钥信息
4. 确保 `keystore.properties` 和您的 `.jks` 文件在 `.gitignore` 中被排除

## 创建新密钥库
如果需要创建新的密钥库，可以使用以下命令：
```bash
keytool -genkey -v -keystore your-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias your-key-alias
```

## 当前状态
- ✅ 已从Git历史中移除敏感文件
- ✅ 已更新 .gitignore 以排除密钥文件
- ✅ 已创建配置模板文件