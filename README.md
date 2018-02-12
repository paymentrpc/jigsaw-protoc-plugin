
jigsaw-protoc-plugin
=======================

参考 https://github.com/os72/protoc-jar-maven-plugin 这个项目。 

### 简单示例

生成java代码到`target/generated-sources`目录, 添加代码到项目中，使用默认的protoc命令， proto文件存放在 `src/main/protobuf` 目录下：
```xml
<plugin>
	<groupId>org.jigsaw.payment</groupId>
	<artifactId>jigsaw-protoc-plugin</artifactId>
	<version>1.0.0</version>
	<executions>
		<execution>
			<phase>generate-sources</phase>
			<goals>
				<goal>run</goal>
			</goals>
		</execution>
	</executions>
</plugin>
```

### 进阶1 

引入`google.protobuf` 标准数据类型, 增加额外的依赖：

```xml
<plugin>
	<groupId>org.jigsaw.payment</groupId>
	<artifactId>jigsaw-protoc-plugin</artifactId>
	<version>1.0.0</version>
	<executions>
		<execution>
			<phase>generate-sources</phase>
			<goals>
				<goal>run</goal>
			</goals>
			<configuration>
				<protocVersion>3.5.0</protocVersion>
				<includeStdTypes>true</includeStdTypes>
				<includeDirectories>
					<include>src/main/more_proto_imports</include>
				</includeDirectories>
				<inputDirectories>
					<include>src/main/protobuf</include>
				</inputDirectories>
			</configuration>
		</execution>
	</executions>
</plugin>
```

### 进阶2

从maven库中下载protoc文件, 支持多种输出格式： 

```xml
<plugin>
	<groupId>org.jigsaw.payment</groupId>
	<artifactId>jigsaw-protoc-plugin</artifactId>
	<version>3.5.0</version>
	<executions>
		<execution>
			<phase>generate-sources</phase>
			<goals>
				<goal>run</goal>
			</goals>
			<configuration>
				<protocArtifact>com.google.protobuf:protoc:3.0.0</protocArtifact>
				<inputDirectories>
					<include>src/main/resources</include>
				</inputDirectories>
				<outputTargets>
					<outputTarget>
						<type>java</type>
					</outputTarget>
					<outputTarget>
						<type>grpc-java</type>
						<pluginArtifact>io.grpc:protoc-gen-grpc-java:1.0.1</pluginArtifact>
					</outputTarget>
				</outputTargets>
			</configuration>
		</execution>
	</executions>
</plugin>
```

### 进阶3

仅编译测试文件， 输出到多个目的目录， 不修改项目内容:
```xml
<plugin>
	<groupId>org.jigsaw.payment</groupId>
	<artifactId>jigsaw-protoc-plugin</artifactId>
	<version>3.5.0</version>
	<executions>
		<execution>
			<phase>generate-test-sources</phase>
			<goals>
				<goal>run</goal>
			</goals>
			<configuration>
				<protocVersion>2.4.1</protocVersion>
				<inputDirectories>
					<include>src/test/resources</include>
				</inputDirectories>
				<outputTargets>
					<outputTarget>
						<type>java</type>
						<addSources>none</addSources>
						<outputDirectory>src/test/java</outputDirectory>
					</outputTarget>
					<outputTarget>
						<type>descriptor</type>
						<addSources>none</addSources>
						<outputDirectory>src/test/resources</outputDirectory>
					</outputTarget>
				</outputTargets>
			</configuration>
		</execution>
	</executions>
</plugin>
```

### 进阶4

使用protoc 2.4.1来编译：

```xml
<plugin>
	<groupId>com.github.os72</groupId>
	<artifactId>protoc-jar-maven-plugin</artifactId>
	<version>3.5.0</version>
	<executions>
		<execution>
			<phase>generate-sources</phase>
			<goals>
				<goal>run</goal>
			</goals>
			<configuration>
				<protocVersion>2.4.1</protocVersion>
				<type>java-shaded</type>
				<addSources>none</addSources>
				<outputDirectory>src/main/java</outputDirectory>
				<inputDirectories>
					<include>src/main/protobuf</include>
				</inputDirectories>
			</configuration>
		</execution>
	</executions>
</plugin>
```

#### Credits

在 [protoc-jar-maven-plugin](https://github.com/os72/protoc-jar-maven-plugin) 基础上开发的。 
