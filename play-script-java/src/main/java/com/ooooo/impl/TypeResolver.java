package com.ooooo.impl;


import com.ooooo.antlr4.PlayScriptBaseListener;
import com.ooooo.antlr4.PlayScriptParser;
import com.ooooo.antlr4.PlayScriptParser.ClassDeclarationContext;
import com.ooooo.antlr4.PlayScriptParser.ClassOrInterfaceTypeContext;
import com.ooooo.antlr4.PlayScriptParser.FormalParameterContext;
import com.ooooo.antlr4.PlayScriptParser.FunctionDeclarationContext;
import com.ooooo.antlr4.PlayScriptParser.FunctionTypeContext;
import com.ooooo.antlr4.PlayScriptParser.PrimitiveTypeContext;
import com.ooooo.antlr4.PlayScriptParser.TypeListContext;
import com.ooooo.antlr4.PlayScriptParser.TypeTypeContext;
import com.ooooo.antlr4.PlayScriptParser.TypeTypeOrVoidContext;
import com.ooooo.antlr4.PlayScriptParser.VariableDeclaratorContext;
import com.ooooo.antlr4.PlayScriptParser.VariableDeclaratorsContext;

/**
 * 第二遍扫描。把变量、类继承、函数声明的类型都解析出来。
 * 也就是所有用到typeTpe的地方。
 * <p>
 * 实际运行时，把变量添加到符号表，是分两步来做的。
 * 第一步，是把类成员变量和函数的参数加进去
 * <p>
 * 第二步，是在变量引用消解的时候再添加。这个时候是边Enter符号表，边消解，会避免变量引用到错误的定义。
 */
public class TypeResolver extends PlayScriptBaseListener {

  private AnnotatedTree at = null;

  //是否把本地变量加入符号表
  private boolean enterLocalVariable = false;

  public TypeResolver(AnnotatedTree at) {
    this.at = at;
  }

  public TypeResolver(AnnotatedTree at, boolean enterLocalVariable) {
    this.at = at;
    this.enterLocalVariable = enterLocalVariable;
  }

  //设置所声明的变量的类型
  @Override
  public void exitVariableDeclarators(VariableDeclaratorsContext ctx) {
    Scope scope = at.enclosingScopeOfNode(ctx);

    //Aaaaaaaaaaayou同学请看这里。
    if (scope instanceof Class || enterLocalVariable) {
      // 设置变量类型
      Type type = (Type) at.typeOfNode.get(ctx.typeType());

      for (VariableDeclaratorContext child : ctx.variableDeclarator()) {
        Variable variable = (Variable) at.symbolOfNode.get(child.variableDeclaratorId());
        variable.type = type;
      }
    }
  }

  //把类成员变量的声明加入符号表
  @Override
  public void enterVariableDeclaratorId(PlayScriptParser.VariableDeclaratorIdContext ctx) {
    String idName = ctx.IDENTIFIER().getText();
    Scope scope = at.enclosingScopeOfNode(ctx);

    //第一步只把类的成员变量入符号表。在变量消解时，再把本地变量加入符号表，一边Enter，一边消解。
    //Aaaaaaaaaaayou同学请看这里。
    //2021-3-4 添加了函数参数，上次修改不小心丢掉了这个场景:-(
    if (scope instanceof Class || enterLocalVariable || ctx.parent instanceof PlayScriptParser.FormalParameterContext) {
      Variable variable = new Variable(idName, scope, ctx);

      //变量查重
      if (Scope.getVariable(scope, idName) != null) {
        at.log("Variable or parameter already Declared: " + idName, ctx);
      }

      scope.addSymbol(variable);
      at.symbolOfNode.put(ctx, variable);
    }
  }

  //设置函数的返回值类型
  @Override
  public void exitFunctionDeclaration(FunctionDeclarationContext ctx) {
    Function function = (Function) at.node2Scope.get(ctx);
    if (ctx.typeTypeOrVoid() != null) {
      function.returnType = at.typeOfNode.get(ctx.typeTypeOrVoid());
    } else {
      //TODO 如果是类的构建函数，返回值应该是一个类吧？

    }

    //函数查重，检查名称和参数（这个时候参数已经齐了）
    Scope scope = at.enclosingScopeOfNode(ctx);
    Function found = Scope.getFunction(scope, function.name, function.getParamTypes());
    if (found != null && found != function) {
      at.log("Function or method already Declared: " + ctx.getText(), ctx);
    }

  }

  //设置函数的参数的类型，这些参数已经在enterVariableDeclaratorId中作为变量声明了，现在设置它们的类型
  @Override
  public void exitFormalParameter(FormalParameterContext ctx) {
    // 设置参数类型
    Type type = at.typeOfNode.get(ctx.typeType());
    Variable variable = (Variable) at.symbolOfNode.get(ctx.variableDeclaratorId());
    variable.type = (Type) type;

    // 添加到函数的参数列表里
    Scope scope = at.enclosingScopeOfNode(ctx);
    if (scope instanceof Function) {    //TODO 从目前的语法来看，只有function才会使用FormalParameter
      ((Function) scope).parameters.add(variable);
    }
  }

  //设置类的父类
  @Override
  public void enterClassDeclaration(ClassDeclarationContext ctx) {
    Class theClass = (Class) at.node2Scope.get(ctx);

    //设置父类
    if (ctx.EXTENDS() != null) {
      String parentClassName = ctx.typeType().getText();
      Type type = at.lookupType(parentClassName);
      if (type != null && type instanceof Class) {
        theClass.setParentClass((Class) type);
      } else {
        at.log("unknown class: " + parentClassName, ctx);
      }
    }

  }


  @Override
  public void exitTypeTypeOrVoid(TypeTypeOrVoidContext ctx) {
    if (ctx.VOID() != null) {
      at.typeOfNode.put(ctx, VoidType.instance());
    } else if (ctx.typeType() != null) {
      at.typeOfNode.put(ctx, (Type) at.typeOfNode.get(ctx.typeType()));
    }
  }


  @Override
  public void exitTypeType(TypeTypeContext ctx) {
    // 冒泡，将下级的属性标注在本级
    if (ctx.classOrInterfaceType() != null) {
      Type type = (Type) at.typeOfNode.get(ctx.classOrInterfaceType());
      at.typeOfNode.put(ctx, type);
    } else if (ctx.functionType() != null) {
      Type type = (Type) at.typeOfNode.get(ctx.functionType());
      at.typeOfNode.put(ctx, type);
    } else if (ctx.primitiveType() != null) {
      Type type = (Type) at.typeOfNode.get(ctx.primitiveType());
      at.typeOfNode.put(ctx, type);
    }

  }

  @Override
  public void enterClassOrInterfaceType(ClassOrInterfaceTypeContext ctx) {
    if (ctx.IDENTIFIER() != null) {
      Scope scope = at.enclosingScopeOfNode(ctx);
      String idName = ctx.getText();
      Class theClass = at.lookupClass(scope, idName);
      at.typeOfNode.put(ctx, theClass);
    }
  }

  @Override
  public void exitFunctionType(FunctionTypeContext ctx) {
    DefaultFunctionType functionType = new DefaultFunctionType();
    at.types.add(functionType);

    at.typeOfNode.put(ctx, functionType);

    functionType.returnType = (Type) at.typeOfNode.get(ctx.typeTypeOrVoid());

    // 参数的类型
    if (ctx.typeList() != null) {
      TypeListContext tcl = (TypeListContext) ctx.typeList();
      for (TypeTypeContext ttc : tcl.typeType()) {
        Type type = (Type) at.typeOfNode.get(ttc);
        functionType.paramTypes.add(type);
      }
    }
  }

  @Override
  public void exitPrimitiveType(PrimitiveTypeContext ctx) {
    Type type = null;
    if (ctx.BOOLEAN() != null) {
      type = PrimitiveType.Boolean;
    } else if (ctx.INT() != null) {
      type = PrimitiveType.Integer;
    } else if (ctx.LONG() != null) {
      type = PrimitiveType.Long;
    } else if (ctx.FLOAT() != null) {
      type = PrimitiveType.Float;
    } else if (ctx.DOUBLE() != null) {
      type = PrimitiveType.Double;
    } else if (ctx.BYTE() != null) {
      type = PrimitiveType.Byte;
    } else if (ctx.SHORT() != null) {
      type = PrimitiveType.Short;
    } else if (ctx.CHAR() != null) {
      type = PrimitiveType.Char;
    } else if (ctx.STRING() != null) {
      type = PrimitiveType.String;
    }

    at.typeOfNode.put(ctx, type);
  }


}