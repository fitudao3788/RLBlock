# RLBlock

Rakuten Link (jp.co.rakuten.mobile.rcs) の着信を番号で拒否する LSPosed モジュール。

## 機能
- 非通知 / 電話帳未登録 / 国外 の着信を拒否(各スイッチで切替)
- 個別の番号を指定して拒否(完全一致、`+81...` は `0...` 扱い)
- 拒否した着信の通知と履歴(日時・番号・理由、最大500件)

## インストール
本モジュールは Xposed API モジュールです。root の有無で手順が異なります。
APK は Releases から入手できます。

### root あり(LSPosed)
1. Magisk / KernelSU などで root 化し、LSPosed(Zygisk 版など)を導入する。
2. 本モジュールの APK(RLBlock)をインストールする。
3. LSPosed マネージャー → モジュール → RLBlock を有効化し、スコープで `jp.co.rakuten.mobile.rcs`(Rakuten Link)にチェックを入れる(スコープは固定)。
4. Rakuten Link を強制停止して再起動する(または端末を再起動)。

### root なし(NPatch)
LSPatch はアーカイブ済みのため、後継の [NPatch](https://github.com/7723mod/NPatch) で Rakuten Link の APK にモジュールを埋め込んで再インストールします。
1. NPatch マネージャーアプリを入手してインストールする。
2. 本モジュールの APK(RLBlock)を通常どおりインストールする(設定画面として使用)。
3. Rakuten Link の APK を用意する(端末から抽出、または自分で入手したもの)。分割 APK(split)の場合は全て選択する。
4. NPatch で Rakuten Link を選び、パッチ方式は「ローカル(Local)」でモジュールとして RLBlock を埋め込んでパッチする。
5. 元の Rakuten Link をアンインストールし、パッチ済み APK をインストールする。
6. パッチ済み Rakuten Link を起動する。

注意:
- 設定画面は libxposed のサービス(LSPosed 等)経由でリモート設定を書き込みます。NPatch 環境でこの接続が成立するかは未検証です。画面上部が「LSPosed 未接続」のままスイッチが操作できない場合、non-root では設定を反映できません。
- パッチ済みアプリは公式版と署名が異なるため、Play ストアからの更新は上書きできません。更新時は再パッチが必要です。
- 動作しない場合は、NPatch 側でモジュールが有効になっているか、RLBlock を再パッチ対象に含めたかを確認してください。
- NPatch の画面や項目名はバージョンにより異なる場合があります。

## 使い方
- **拒否ルール**: 非通知 / 電話帳未登録 / 国外 の各スイッチ(変更は即時保存・リモート設定経由で反映)。
- **個別の番号**: 追加、スワイプ右=有効/無効、左=削除、長押しで複数選択。
- **ブロック履歴**: 長押しで複数選択、スワイプで削除。
- **通知と履歴**: ブロック時の通知(Android 13+ は通知権限が必要)と履歴記録をそれぞれオフにできます。
- **ログ**: LSPosed のログで `RLBlock` を検索(`rejected(理由): ...`)。

### 判定順
個別番号 → 非通知 → (電話帳登録済みなら許可) → 国外 → 未登録。
連絡先の照会に失敗した場合は拒否せず通します。

## 仕組み
- フック先: `CallSlInteractor` の `(String,String,String,String,boolean,boolean)V` メソッド(難読化名 `c`、シグネチャで特定)。引数1=callId、引数2=発信者URI、引数6=ネイティブ着信フラグ。
- ブロック対象なら `ApiServiceMain.rejectCall(callId, 603)` を呼び、元処理をスキップ(着信UI・呼出音は出ない)。
- ネイティブ着信(引数6=true)は対象外。
- 拒否した着信はフック側からモジュールへ明示ブロードキャスト(`work.fitudao.rlblock.ACTION_BLOCKED`)で通知し、履歴に記録します。
- Rakuten Link の更新でクラス構成が変わると動作しなくなる可能性があります。

## ビルド
Android Studio で開くか `./gradlew assembleRelease`。

## ライセンス
[Apache License 2.0](LICENSE)
