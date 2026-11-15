# RLBlock

Rakuten Link (`jp.co.rakuten.mobile.rcs`) の着信を番号で拒否する LSPosed モジュール。

動作確認済み: Rakuten Link 4.0.1

## 機能
- 非通知 / 電話帳未登録 / 国外 の着信を拒否
- 個別の番号を指定して拒否(完全一致、`+81...` は `0...` 扱い)
- 拒否した着信の通知と履歴(最大500件)

## インストール
APK は [Releases](https://github.com/fitudao3788/RLBlock/releases) から入手できます。

### root あり(LSPosed)
1. LSPosed を導入し、RLBlock の APK をインストールする。
2. LSPosed マネージャーで RLBlock を有効化し、スコープに `jp.co.rakuten.mobile.rcs` を選ぶ。
3. Rakuten Link を強制停止して再起動する。

### root なし(NPatch)
LSPatch はアーカイブ済みのため、後継の [NPatch](https://github.com/7723mod/NPatch) を使います。
1. NPatch と RLBlock の APK をインストールする。
2. NPatch で Rakuten Link の APK(分割 APK は全て選択)を、「ローカル」方式で RLBlock を埋め込んでパッチする。
3. 元の Rakuten Link をアンインストールし、パッチ済み APK をインストールする。

注意:
- 設定画面は LSPosed 等の libxposed サービス経由で設定を書き込みます。NPatch でこの接続が成立するかは未検証です。画面上部が「LSPosed 未接続」のままなら、non-root では設定を反映できません。
- パッチ済みアプリは公式版と署名が異なるため、アプリの更新ごとに再パッチが必要です。

## 使い方
- **拒否ルール**: 非通知 / 電話帳未登録 / 国外 のスイッチ。変更は即時反映されます。
- **個別の番号**: 追加、スワイプ右=有効/無効、左=削除、長押しで複数選択。
- **ブロック履歴**: 長押しで複数選択、スワイプで削除。
- **通知と履歴**: 通知(Android 13+ は通知権限が必要)と履歴記録を個別にオフにできます。
- **ログ**: LSPosed のログで `RLBlock` を検索。

判定順: 個別番号 → 非通知 → (電話帳登録済みなら許可) → 国外 → 未登録。連絡先の照会に失敗した場合は拒否せず通します。

## 仕組み
- `CallSlInteractor` の着信メソッド(難読化名 `c`、シグネチャ `(String,String,String,String,boolean,boolean)V` で特定)をフックし、ネイティブ着信を除く着信を判定します。
- 拒否時は `ApiServiceMain.rejectCall(callId, 603)` を呼び、元処理をスキップします(着信UI・呼出音は出ません)。
- 履歴はフック側から明示ブロードキャストでモジュールへ通知して記録します。
- Rakuten Link の更新でクラス構成が変わると動作しなくなる可能性があります。

## ビルド
`./gradlew assembleRelease`(または Android Studio)。

## ライセンス
[Apache License 2.0](LICENSE)
