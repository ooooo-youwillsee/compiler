# compiler

## project

* craft-ast:

手写AST，实现一个基础的计算脚本

* play-script-java

`PlayScriptCompiler`： 入口类

mac:

```shell
# 设置环境变量
export CLASSPATH=".:/Users/ooooo/Code/Self/compiler/play-script-java/libs/antlr-4.9.3-complete.jar:$CLASSPATH"
alias antlr4='java -Xmx500M -cp "$CLASSPATH" org.antlr.v4.Tool'
alias grun='java -Xmx500M -cp "$CLASSPATH" org.antlr.v4.gui.TestRig'

# 生成 AST 代码
antlr4 PlayScript.g4 -o /Users/ooooo/Code/Self/compiler/play-script-java/src/main/java/com/ooooo/antlr4 -visitor
```

windows:

```shell
SET CLASSPATH=.;C:\Javalib\antlr-4.10.1-complete.jar;%CLASSPATH%
```