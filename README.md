# Jubatus On YARN

(English follows Japanese)

## 日本語

### 納品物一覧

- ./document/

  ドキュメント

  - ビルド・利用手順書
  - テスト結果報告書
  - Jubatus-YARNアプリケーション向け検証用クラスタ セットアップ方法

- ./jubatusonyarn/

  ソースコード

  - jubatus-on-yarn-application-master/

    ApplicationMaster プロジェクト（juba*_proxy と1対1で起動し、Container を管理します）

  - jubatus-on-yarn-client/

    メインプロジェクト（ApplicationMaster を管理します）

  - jubatus-on-yarn-common/

    共通プロジェクト

  - jubatus-on-yarn-container/

    Container プロジェクト（juba* と1対1で起動します）

  - jubatus-on-yarn-test/

    結合試験用プロジェクト


具体的な利用方法や実行方法は「ビルド・利用手順書」を参照してください。

## English

### Contents Overview

- ./document/

  Documentation

  - Compile and use instructions
  - Test description and results
  - Cluster setup instructions

- ./jubatusonyarn/

  Source code

  - jubatus-on-yarn-application-master/

    Application Master project (1:1 relation with juba*_proxy, manages containers)

  - jubatus-on-yarn-client/

    Main project (manages the Application Master)

  - jubatus-on-yarn-common/

    Shared project

  - jubatus-on-yarn-container/

    Container project (1:1 relation with juba* instances)

  - jubatus-on-yarn-test/

    Integration test code

The concrete usage instructions are in the document called "ビルド・利用手順書".

