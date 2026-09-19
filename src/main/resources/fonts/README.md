# 字体文件说明

## Noto Sans SC

**来源**: Google Fonts — https://github.com/google/fonts/tree/main/ofl/notosanssc
**许可证**: SIL Open Font License 1.1 (OFL-1.1)，全文见同目录 `OFL-NotoSansSC.txt`

OFL-1.1 允许商业使用、修改、分发与**嵌入到应用程序中**。

### 文件清单与用途

| 文件 | 类型 | 用途 |
|---|---|---|
| `NotoSansSC-Regular.ttf` | TrueType（`glyf` 轮廓） | **PDF 导出必须使用它**（JavaFX 界面亦可） |
| `NotoSansSC-Bold.ttc` / `NotoSansSC-Regular.ttc` | OTF/CFF 字体集合 | **仅供 JavaFX 界面**；PDFBox 无法嵌入（见下） |
| `fontawesome-webfont.ttf` | TrueType 图标字体 | 界面图标 |

### 为什么宁可多内置一个 10MB 的 `.ttf`

PDFBox 只能嵌入 **TrueType(glyf)** 字体。仓库里原有的 `NotoSansSC-Regular.ttc` 实际是
**Noto Sans CJK 的 OTF/CFF 版本**（子字体名形如 `NotoSansCJKsc-Regular`），没有 `glyf` 表：

- 用 `embedSubset=false` 整体嵌入 → `IOException: Full embedding of TrueType font collections not supported`
- 用 `embedSubset=true` 子集化 → `PDDocument.save()` 时 `UnsupportedOperationException: OTF fonts do not have a glyf table`

因此它**不能用于 PDF 导出**。macOS / Windows 过去是靠系统里的 TrueType 中文字体
（如 `Arial Unicode.ttf`）蒙混过去；Linux（含 CI 的 Ubuntu runner）默认没有中文字体，
就会报 `无法加载中文字体`。内置 `NotoSansSC-Regular.ttf` 后，PDF 导出不再依赖系统字体。

### `NotoSansSC-Regular.ttf` 的来历（可复现）

Google Fonts 只提供**变量字体** `NotoSansSC[wght].ttf`，其 `fvar` 轴 `wght`
**默认值是 100（Thin）**，直接内置会得到细体报表。因此先固化成静态 Regular：

```bash
# 1) 取变量字体（约 17MB）
curl -L -o NotoSansSC[wght].ttf \
  "https://raw.githubusercontent.com/google/fonts/main/ofl/notosanssc/NotoSansSC%5Bwght%5D.ttf"

# 2) 用 fontTools 生成 wght=400 的静态实例（约 10MB，gvar/fvar 被移除）
pip install fonttools
fonttools varLib.instancer "NotoSansSC[wght].ttf" wght=400 -o NotoSansSC-Regular.ttf
```

（另将 name 表的 family/subfamily/PostScript 名改为 `Noto Sans SC` / `Regular` /
`NotoSansSC-Regular`，避免元数据仍写 Thin；OFL 的 Reserved Font Name 是 `Source`，未被使用。）

### 代码侧的约定

- `ExportUtil.loadChineseFont()` 的候选顺序是：系统字体 → 文件系统 → 项目资源，
  其中 `.ttf` 排在 `.ttc` **之前**，避免白白解析 19MB 的 CFF 集合。
- `isEmbeddable()` 会跳过没有 `glyf` 表的候选；`canRender()` 会跳过缺少数字/中文覆盖的候选
  （例如 `Droid Sans Fallback` 只有 CJK 字形、渲染数字会抛 `No glyph for U+0031`）。
- 若替换该字体，请同步更新 `OFL-NotoSansSC.txt` 并在本节记录来源与生成方式。

---

**注意**: 若需替换字体，请确认其许可证允许嵌入与再分发，并保证是 **TrueType(glyf)** 且覆盖
数字与中文，否则 PDF 导出会在保存阶段失败。
