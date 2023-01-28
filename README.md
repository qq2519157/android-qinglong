# 青龙面板APP

## 1.软件介绍

青龙面板是支持python3、javaScript、shell、typescript 的定时任务管理面板。本APP基于青龙面板***2.10.13***
接口开发，支持面板大部分原生功能，同时提供拓展模块，帮助用户快捷管理。

## 2.使用环境

1. 安卓***8.0***及以上手机 ；
2. 面板版本***2.10.x***；

## 3.功能介绍

### 3.1.基础功能

提供定时任务、环境变量、配置文件、脚本管理、依赖管理、任务日志功能。

1. 定时任务：支持增改删查、批量操作、日志查看、任务去重、本地备份和本地导入；
2. 环境变量：支持增改删查、批量操作、日志查看、快捷导入、远程导入、本地导入、本地备份和变量去重；
3. 配置文件：支持查看配置和修改配置；
4. 脚本管理：支持查看脚本和修改脚本；
5. 依赖管理：支持依赖增删、批量操作和日志查看；
6. 任务日志：支持查看日志列表；

备注：
1. 部分编辑功能需要长按详细信息位置唤醒；
2. 变量远程导入文件内容格式可参考***examples/envs.json***
   文件，测试地址为https://gitee.com/wsfsp4/QingLong/raw/master/examples/envs.json；
3. 本地备份文件将保存在外部存储***qinglong***文件夹下；
4. 本地导入将在***qinglong***文件夹下查找对应模块的文件，可自行参考备份文件的内容格式创建新文件，文件名不要求，以json作为文件后缀即可；


### 3.2.拓展模块

#### 3.2.1 CK助手

提供网页cookies提取和导入变量功能。

备注：
1. 导入变量功能需要先配置规则，支持手动添加或远程导入；
2. 规则远程导入文件内容格式可参考examples/rules.json文件，测试地址为https://gitee.com/wsfsp4/QingLong/raw/master/examples/rules.json；

##### 3.2.1.1 规则配置个字段说明

1. 环境变量：同面板的环境变量，由字母、数字和下划线组成；
2. 规则名称：该规则的名称，仅供用户识别用；
3. 网址：输入框原始加载的网址；
4. 目标键：要从cookies中提取的键值,支持以下4种格式,假如网页cookies为"a=1;b=2;c=3;"，连接符为";"： 
   | 格式 | 说明 | 值 |
   |----------|----------|----------| 
   | * | 提取cookies全部键值 | a=1;b=2;c=3; | 
   | *;a>>aa;b>>bb | 提取cookies全部键值，并将a键重命名为aa,b重命名为bb | aa=1;bb=2;c=3; | 
   | a=;b>>bb= | 提取cookies中a键值和b键值，并将b键重命名为bb | a=1;bb=2; | 
   | a;b | 提取cookies中a值和b值 | 1;2; |
5. 主键：cookies中的键，其值将作为面板环境变量的备注，匹配规则成功时若环境变量该值相同则进行更新操作，否则进行新建变量操作；
6. 连接符：拼接目标键值的符号，支持';'、'#'、'&'、'@'和'%'符号;

##### 3.2.1.2 规则匹配说明

1. 匹配时，遍历启用的所有规则，匹配成功则停止；
2. 如果规则中提取具体字段，只要一个字段不存在将匹配失败；
3. 规则中的网址只和原始加载的网址相比较，尽管加载后用户点击页面跳转到其他页面；

## 4.意见反馈

APP还在开发，后续尝试加入更多拓展模块,提供更加强大的功能，有问题和功能需求可以提issue，有空就解决。

