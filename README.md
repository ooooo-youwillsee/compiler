# compiler

## project

* craft-ast:

手写 AST，实现一个简单的计算器

* antlr-ast:

使用 antlr4 来实现 AST，实现一个简单的计算器

```shell
# 生成 AST 代码
antlr4 CommonLexer.g4 -o /Users/ooooo/Code/Self/compiler/antlr-ast/src/main/java/com/ooooo/antlr4 -visitor
antlr4 PlayScript.g4 -o /Users/ooooo/Code/Self/compiler/antlr-ast/src/main/java/com/ooooo/antlr4 -visitor

```

* play-script-java

`PlayScriptCompiler`： 入口类

mac:

```shell
# 设置环境变量
export CLASSPATH=".:/Users/ooooo/Code/Self/compiler/libs/antlr-4.9.3-complete.jar:$CLASSPATH"
alias antlr4='java -Xmx500M -cp "$CLASSPATH" org.antlr.v4.Tool'
alias grun='java -Xmx500M -cp "$CLASSPATH" org.antlr.v4.gui.TestRig'

# 生成 AST 代码
antlr4 PlayScript.g4 -o /Users/ooooo/Code/Self/compiler/play-script-java/src/main/java/com/ooooo/antlr4 -visitor
```

windows:

```shell
SET CLASSPATH=.;C:\Javalib\antlr-4.10.1-complete.jar;%CLASSPATH%
```