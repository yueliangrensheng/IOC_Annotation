package com.yazao.lib.annotation.compiler;

import com.google.auto.service.AutoService;
import com.yazao.lib.annotation.BindView;
import com.yazao.lib.annotation.OnClick;

import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * from hymanAndroid - ioc-apt-sample
 */
@AutoService(Processor.class)
public class ViewInjectProcessor extends AbstractProcessor {
    private Filer filer;
    private Elements elements;
    private Messager messager;

    private Map<String, ProxyInfo> mProxyMap = new HashMap<>();

    /*
        Filer :  文件辅助类；用于生成 JavaSourceCode

        Elements : 元素辅助类；用于获取元素相关信息
           子类如下
            - VariableElement //代表 成员变量
            - TypeElement //代表 类
            - ExecutableElement //代表 类中的方法
            - PackageElement //代表 package

        Messager  : 日志相关类；

     */

    /**
     * init 方法  -- 初始化辅助类信息
     *
     * @param processingEnvironment
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        filer = processingEnvironment.getFiler();
        elements = processingEnvironment.getElementUtils();
        messager = processingEnvironment.getMessager();

    }

    /**
     * 该方法 固定写法 -- add 支持的注解类
     *
     * @return
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<>();
        annotations.add(BindView.class.getCanonicalName());
        return annotations;
    }

    /**
     * 该方法 固定写法 -- 支持的源码版本
     *
     * @return
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * <p>
     * process中实现较复杂，一般认为可以有两大步骤：
     * 1. 收集信息
     * 2. 生成代理类
     * <p>
     * <p>
     * 关于收集信息：
     * 就是根据你的注解申明，拿到对应的Element，然后获取我们需要的信息，这个信息是为后面生成JavaFileObject所准备的。
     * <p>
     * 关于生成代理类：
     * 会针对每个类生成一个代理类，，如果有多个类中声明了注解，就回生成多个代理类，辣么需要作如下处理
     * 1.一个类对象 代表具体某个类的代理类生成的全部信息 {@link ProxyInfo}
     * 2.一个集合，存放1中的代理类。Map<String, ProxyInfo>，key为类的全路径
     * </p>
     *
     * @param set
     * @param roundEnvironment
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {

        messager.printMessage(Diagnostic.Kind.NOTE, "------ process ------");

        //因为process可能会被多次调用，避免生成重复的代理类
        mProxyMap.clear();

        // 1. 收集信息
        //1.1 获取 被 @BindView注解的元素；这里被 @BindView 的都是 变量（用来View的findViewById）；
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(BindView.class);

        for (Element element : elements) {

//            //TODO 检查 element 是否是 ExecutableElement -- click事件
//            if (element.getKind() == ElementKind.METHOD) {
//
//            }

            System.out.println("ViewInjectProcessor_process : element.getKind() = " + element.getKind().name());
            //检查 element 是否是 VariableElement -- 成员变量
            //使用这个 ElementKind.FIELD 是因为我们的 BindView 仅仅使用在 成员变量 xxView 上（用来查找view的id）
            if (element.getKind() == ElementKind.FIELD) {
                // if (element instanceof TypeElement){} // 尽量禁用这种判断，因为 TypeElement 即是一个类也是一个接口
                // if (element.getKind() == ElementKind.CLASS) //正确的打开方式


                // field type
                VariableElement variableElement = (VariableElement) element;
                // class type
                TypeElement classElement = (TypeElement) variableElement.getEnclosingElement();// getEnclosingElement(): 返回元素的父元素(即元素所在的类)
                // full class name
                String qualifiedName = classElement.getQualifiedName().toString();

                ProxyInfo proxyInfo = mProxyMap.get(qualifiedName);
                if (proxyInfo == null) {
                    proxyInfo = new ProxyInfo(this.elements, classElement);
                    mProxyMap.put(qualifiedName, proxyInfo);
                }

                BindView annotation = variableElement.getAnnotation(BindView.class);
                int id = annotation.value();

                proxyInfo.injectVariables.put(id, variableElement);

            }

        }

        //1.2 获取被 @OnClick 注解的元素；
        Set<? extends Element> elementsClicks = roundEnvironment.getElementsAnnotatedWith(OnClick.class);


        // 2. 生成代理类
        // 核心的代理类生成是在 ProxyInfo 类内部实现
        for (String key : mProxyMap.keySet()) {
            if (key == null || key.length() == 0 || key.trim().length() == 0) {
                continue;
            }

            ProxyInfo proxyInfo = mProxyMap.get(key);
            if (proxyInfo == null) {
                continue;
            }

            try {
                JavaFileObject sourceFile = filer.createSourceFile(proxyInfo.getProxyClassFullName(), proxyInfo.getTypeElement());
                Writer writer = sourceFile.openWriter();
                writer.write(proxyInfo.generateJavaCode());
                writer.flush();
                writer.close();
            } catch (Exception e) {
                messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            }
        }

        return true;
    }
}
