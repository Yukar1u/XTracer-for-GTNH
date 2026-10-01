# XTracer 手持终端轻量化 NEI 风格选择器方案

## 1. 文档目的

本文将 XTracer 手持终端后续方向收束为可实施方案。目标是在终端 GUI 中提供接近 NEI 使用习惯的物品/方块可视化目录和检索功能，逐步替代手动输入 `/xtracer block`、`/xtracer ore`，同时保持 XTracer 的核心语义：**选择一个可追踪目标，然后由服务端扫描世界中的方块**。

本文只描述方案，不改变当前已经可用的终端、扫描和命令代码。

## 2. 总体决策

### 2.1 依赖 NEI API，不复制 NEI

XTracer 不搬运 NEI 源码，也不复制 NEI 的 GUI、缓存或内部类。推荐采用“可选集成、独立界面”的方式：

- NEI 作为 GTNH 环境中的外部模组提供物品信息和交互参考。
- XTracer 通过 NEI 的公开/稳定 API 获取可用的物品、方块和显示信息。
- XTracer 自己实现终端内的目录、搜索框、分页、目标选择和布局。
- XTracer 不依赖 NEI 的内部页面类、私有字段、内部事件顺序或具体主题样式。
- NEI 缺失时，XTracer 使用 Minecraft/Forge 注册表和 Ore Dictionary 构建基础目录，终端仍可工作。
- NEI 存在时，优先使用 NEI 提供的物品显示名称、图标和候选信息，但不把 NEI 的对象直接作为服务端可信目标。

这样可以减少复制代码带来的许可证、版本同步和维护成本，也避免 NEI 内部重构直接破坏终端 GUI。

### 2.2 目录显示范围与扫描能力分离

目录可以展示大量物品，但“可展示”不等于“可作为当前扫描目标”。首版应明确区分：

- **可追踪目标**：能够稳定映射到世界方块匹配规则的条目。
- **仅供浏览**：可以显示和搜索，但当前扫描器不能直接处理的普通物品。
- **特殊目标**：需要独立扫描或处理逻辑，暂不纳入首版。

用户点击条目后，GUI 只提交规范化目标描述；服务端再次验证并决定是否接受。客户端目录不拥有扫描权限。

## 3. 目标数据模型

目录条目不直接保存任意 `ItemStack` 作为网络协议，而是转换为可验证的目标引用。

### 3.1 Block 目标

用于普通世界方块：

```text
kind = BLOCK
registryName = modid:block_name
meta = 0..15
```

服务端保存时继续兼容当前配置格式：

```text
modid:block_name@meta
```

目录条目必须能够从 `ItemStack` 反查对应 `Block`，并确认注册名存在。不能反查为方块的物品显示为不可选择或仅浏览。

### 3.2 Ore Dictionary 目标

用于矿辞匹配：

```text
kind = ORE_DICT
oreName = oreIron
```

客户端可以展示该矿辞对应的代表性图标和候选数量，但服务端只接收矿辞名称，并通过 `OreDictionary.doesOreNameExist` 与 `OreDictionary.getOres` 重新验证。

现有矿辞扫描语义保持不变：根据世界方块的掉落物提取 Ore Dictionary 名称进行匹配。

### 3.3 GregTech 掉落目标

用于当前 `gtName` 逻辑：

```text
kind = DROP_ITEM
itemName = gregtech:item_name
meta = damage
```

第一阶段不要求目录直接完整支持该类型。后续可从条目的代表性 `ItemStack` 生成目标，并由服务端验证其物品注册名和 damage。服务端扫描仍然通过方块掉落物匹配，而不是直接扫描地面物品实体。

### 3.4 普通物品

普通不可放置物品可以出现在目录和搜索结果中，但首版标记为“不可用于方块追踪”。不允许用户选择后得到一个必然为零结果的目标，也不通过特殊硬编码把任意物品强行映射为方块。

后续若需要追踪掉落物实体、容器内容或机器产物，应新增明确的扫描模式，而不是改变 `BLOCK` 目标的含义。

## 4. NEI 交集模式的具体实现

### 4.1 依赖配置

项目的 `dependencies.gradle` 已提供 `compileOnly`/可选依赖入口。NEI 集成采用以下原则：

- 只在编译期声明 NEI API 或开发环境可用的 NEI 依赖。
- 不把 NEI 打包进 XTracer JAR。
- 不把 NEI 作为 XTracer 的硬性运行时依赖。
- 通过 Forge 模组加载状态或类存在性判断 NEI 是否可用。
- 所有 NEI 引用集中放在 `integration/nei` 包内，公共 GUI 和服务端类不直接导入 NEI 类型。

建议结构：

```text
integration/
  nei/
    NeiIntegration.java
    NeiItemSource.java
    NeiTooltipProvider.java
```

`NeiIntegration` 只提供 XTracer 需要的窄接口，例如：

```java
interface ItemCatalogSource {
    List<CatalogEntry> getEntries();
    List<String> getSearchText(ItemStack stack);
    void appendTooltip(ItemStack stack, List<String> tooltip);
}
```

公共 GUI 依赖 `ItemCatalogSource`，不依赖 NEI 的类名。NEI 不存在时注入 `VanillaCatalogSource`，从 Forge/Minecraft 注册表构建基础数据。

### 4.2 数据源优先级

启动或第一次打开终端时选择数据源：

1. NEI 集成可用：使用 NEI 提供的可浏览物品集合和显示辅助信息。
2. NEI 不可用：使用 `Item.itemRegistry`、`Block.blockRegistry`、`OreDictionary` 和本地化名称构建目录。
3. 任一数据源异常：跳过异常条目，保留其他条目；不能因为一个坏条目导致整个 GUI 无法打开。

NEI 数据源只在客户端使用。服务器不接收完整目录，也不信任客户端上传的 `ItemStack`、NBT、类名或对象引用。

### 4.3 条目规范化与去重

客户端把 NEI/注册表返回的内容规范化为 `CatalogEntry`：

```text
entryId
kind: BLOCK | ORE_DICT | DROP_ITEM | BROWSE_ONLY
displayName
registryName
meta
oreNames[]
searchText[]
representativeStack
trackable
```

去重规则：

- `BLOCK + registryName + meta` 唯一。
- 同一方块的多个 Ore Dictionary 名称不创建多个方块条目；矿辞作为搜索字段或独立 Ore Dictionary 条目。
- 相同物品和 damage 的不同 NBT 默认合并为一个条目，除非未来明确需要 NBT 目标。
- 没有稳定注册名的动态条目只允许浏览，不允许提交为追踪目标。

### 4.4 搜索索引

目录第一次构建后建立客户端索引，不在每次按键时遍历所有注册表：

- 注册名索引：`modid:block_name`、`modid:item_name`。
- 本地化名称索引：用于普通玩家搜索。
- Ore Dictionary 索引：用于 `oreIron`、`ingotCopper` 等关键词。
- GregTech 名称索引：用于后续 `DROP_ITEM` 支持。
- 预处理为小写并统一空格、下划线和冒号的匹配形式。

搜索采用包含匹配即可作为首版；后续可加前缀优先级和模糊匹配。过滤结果只保留当前分页需要绘制的条目。

### 4.5 GUI 布局

终端 GUI 推荐分为三块：

1. **搜索与过滤栏**
   - 搜索输入框。
   - 模式过滤：全部、方块、Ore Dictionary、可追踪。
   - 可选分类过滤：按 NEI/物品分类信息过滤。
2. **目录网格**
   - 使用 `ItemStack` 图标绘制。
   - 每格显示简短名称或 hover tooltip。
   - 只绘制当前页，例如 8 列 × 5 行。
   - 鼠标滚轮或上下页按钮切换页面。
3. **当前目标与操作栏**
   - 当前目标类型、名称、meta。
   - 半径输入或步进控制。
   - `getDrop` 开关。
   - Ray Select 和 Scan 按钮。
   - 不可追踪条目显示原因，不发送无效请求。

GUI 不追求复刻 NEI 的全部页面、配方树或用法页面；首版只借鉴 NEI 的“搜索 + 图标网格 + tooltip + 点击选择”交互。

### 4.6 点击条目流程

1. 客户端点击 `CatalogEntry`。
2. GUI 根据条目类型生成规范化请求：
   - 方块：`BLOCK + registryName + meta`
   - Ore Dictionary：`ORE_DICT + oreName`
   - GregTech 掉落物：未来使用 `DROP_ITEM + itemName + meta`
3. GUI 立即更新本地“待提交目标”显示，但不直接修改服务端配置。
4. 发送 C2S 请求。
5. 服务端重新查找注册表、验证模式和参数。
6. 服务端接受后更新玩家配置并返回成功状态；拒绝则返回错误，GUI 恢复此前已确认目标。

## 5. 服务端网络协议

### 5.1 目录不下发

完整目录由客户端构建，不通过网络传输。原因：

- 目录条目数量大，没必要每次打开同步。
- 客户端与服务端的注册表可能有差异时，服务端必须以自身注册表为准。
- 减少网络包大小和协议复杂度。

### 5.2 目标设置请求

在现有 `TerminalRequestMessage` 基础上扩展明确的操作和目标类型，避免继续依赖命令字符串：

```text
operation = SET_TARGET
kind = BLOCK | ORE_DICT | DROP_ITEM
registryName / oreName
meta
```

服务端约束：

- 字符串长度限制，例如 128 字节。
- `kind` 必须是白名单值。
- Block 必须通过 `GameRegistry.findBlock` 或对应注册表确认存在。
- meta 限制为合法范围。
- Ore Dictionary 必须存在且有实际条目。
- DROP_ITEM 仅在该功能实现后开放，不能由客户端自行声称目标有效。
- 服务端从消息上下文取得玩家，不接受客户端 UUID、坐标或权限字段。

## 6. 掉落与特殊方块策略

### 6.1 总原则

优先实现核心使用场景，不为一般玩家可以明确规避的边缘情况增加大量硬编码。`getDrop` 是高风险操作，应保持为显式开关，而不是默认行为。

不要把逻辑写成大量“某个具体方块走某个特判”。应按行为能力建模：

- 普通方块扫描/掉落逻辑。
- 可选的精准采集逻辑。
- 永久性作物的收割而非破坏逻辑。
- 未来针对容器/机器状态的通用能力。

### 6.2 精准采集开关

后续增加 `silkTouch` 或“精准采集”开关：

- 关闭时使用正常掉落逻辑。
- 打开时使用等价于精准采集的掉落计算/破坏路径。
- 该设置由 GUI 控制并由服务端验证。
- 不把精准采集行为硬编码为某个方块的专属例外。

具体实现需确认 1.7.10/GTNH 的掉落 API 是否能稳定模拟工具和附魔；在确认前，不承诺对所有模组方块完全等同玩家手挖。

### 6.3 永久性作物

永久性作物单独设计为“收割作物”模式：

- 只收取成熟产物。
- 保留作物方块和必要的生长状态。
- 不使用普通 `setBlockToAir` 破坏路径。
- 通过作物能力/成熟度接口或可验证的通用行为判断处理，而不是写死某个作物注册名。
- 首版不实现，除非目标用户和测试样本明确需要。

### 6.4 容器和机器

容器内物品丢失是已知行为，但不作为首版阻塞项：

- 普通扫描不受影响。
- `getDrop` 开启前 GUI 应明确提示会移除匹配方块并生成掉落。
- 容器、机器、TileEntity 状态保留和库存转移作为后续通用能力。
- 不为单个容器方块写专属白名单或特判。
- 后续若实现，应基于可识别的库存/TileEntity 能力，把容器内容安全转移或明确跳过，而不是静默丢失。

## 7. 分阶段落地顺序

### 阶段 A：基础目录替代 Block 输入

- 客户端注册表构建 Block 目录。
- 搜索框、图标网格、分页和 tooltip。
- 点击条目生成 `BLOCK` 目标。
- 服务端重新校验并写入现有 `targetList`。
- 保留 `/xtracer block` 作为兼容入口。

### 阶段 B：Ore Dictionary 目录

- 构建 Ore Dictionary 条目和反向搜索索引。
- 点击矿辞条目生成 `ORE_DICT` 目标。
- 展示代表性图标和候选数量。
- 服务端重新校验矿辞。
- 保留 `/xtracer ore` 作为兼容入口。

### 阶段 C：NEI 可选集成

- 增加 `integration/nei` 适配层。
- 通过 `compileOnly` 或项目认可的可选开发依赖编译。
- NEI 存在时读取其稳定物品候选/显示辅助信息。
- NEI 缺失时自动回退到 Forge/Minecraft 目录源。
- 不把 NEI API 类型泄漏到公共网络、配置或服务端类。

### 阶段 D：GregTech 掉落目标

- 将 GT 掉落 ItemStack 转换为 `DROP_ITEM`。
- 支持名称、damage 和 tooltip 说明。
- 服务端通过实际掉落匹配，不把普通物品误当方块。
- 用真实 GT 方块样本验证多掉落、空掉落和不同 damage。

### 阶段 E：可选采集能力

- 增加精准采集开关。
- 设计永久性作物收割模式。
- 对 `getDrop` 增加确认、冷却、结果统计和必要的保护。
- 容器/机器状态保留仅在明确需求出现后实现。

## 8. 验收标准

### 目录与搜索

- 首次打开终端不会卡顿到不可用；目录只构建一次并缓存。
- 搜索本地化名称、注册名和 Ore Dictionary 名称均可找到对应条目。
- 分页、滚轮、tooltip 和点击选择正常。
- 不可追踪物品会被标记或过滤，不会提交为无效目标。

### 网络与安全

- 客户端不能通过伪造 `ItemStack`、NBT、UUID、坐标或任意类名绕过服务端校验。
- 非法注册名、meta、矿辞名称和未知目标类型被拒绝。
- 目录本身不通过网络大包同步。
- 多人服务器中玩家目标配置互不影响。

### 掉落行为

- 默认不破坏方块。
- `getDrop` 必须由玩家明确开启。
- 精准采集和永久作物逻辑是独立开关/模式，不隐藏改变普通扫描语义。
- 容器状态不在首版承诺；GUI 和帮助文本说明 `getDrop` 的破坏性行为。

## 9. 当前结论

推荐采用：

> **XTracer 自己实现轻量 NEI 风格终端 GUI，NEI 只作为可选数据/显示集成，不复制 NEI；目录支持 Block 和 Ore Dictionary 目标，普通物品只浏览不可追踪；服务端重新验证所有目标；掉落能力按通用行为开关演进，精准采集和永久作物后置，容器状态处理暂缓。**

该方案能降低长期维护成本，同时保留 GTNH/NEI 环境下熟悉的搜索体验，并避免把目录展示、目标语义和世界破坏逻辑混成一套不可控的特判系统。
