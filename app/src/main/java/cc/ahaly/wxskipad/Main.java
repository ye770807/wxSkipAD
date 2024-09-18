package cc.ahaly.wxskipad;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import java.lang.reflect.Method;
import java.util.Arrays;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Main implements IXposedHookLoadPackage {


    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("com.tencent.mm")) {
            String logMessage = "钩住微信包 Hooked into WeChat package: " + lpparam.packageName;
            sendLogBroadcast("Initial Log", logMessage);

            //打印所有加载的类
//            XposedHelpers.findAndHookMethod(
//                    ClassLoader.class,
//                    "loadClass",
//                    String.class,
//                    boolean.class,
//                    new XC_MethodHook() {
//                        @Override
//                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                            String className = (String) param.args[0];
//                            sendLogBroadcast("Class Loaded: " , className); // 打印加载的类
//                        }
//                    });

            // 钩住TinkerApplication的attachBaseContext
            Class<?> tinkerAppClass = XposedHelpers.findClass("com.tencent.tinker.loader.app.TinkerApplication", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(tinkerAppClass, "attachBaseContext", Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    ClassLoader classLoader = ((Context) param.args[0]).getClassLoader();
                    sendLogBroadcast("类加载器ClassLoader: " , classLoader.toString());
                    hookAdClasses(classLoader);
                }
            });
        }
    }

    // 钩住广告类的方法
    private void hookAdClasses(ClassLoader classLoader) {

        // 钩住你提到的广告类
        String[] adUiClasses = {
                "com.tencent.mm.plugin.finder.feed.ui.FinderLiveVisitorWithoutAffinityUI",
                "com.tencent.mm.plugin.appbrand.ui.AppBrandUI"
//                "com.tencent.mm.plugin.appbrand.ui.AppBrandUI2",
//                "com.tencent.mm.plugin.appbrand.ui.AppBrandUI3",
//                "com.tencent.mm.plugin.appbrand.ui.AppBrandUI4",
//                "com.tencent.mm.plugin.appbrand.ad.ui.AppBrandAdUI",
//                "com.tencent.mm.plugin.appbrand.ad.ui.AppBrandAdUI1",
//                "com.tencent.mm.plugin.appbrand.ad.ui.AppBrandAdUI2",
//                "com.tencent.mm.plugin.appbrand.ad.ui.AppBrandAdUI3",
//                "com.tencent.mm.plugin.appbrand.ad.ui.AppBrandAdUI4",
//                "com.tencent.mm.plugin.appbrand.ui.recommend.AppBrandRecommendVideoUI",
//                "com.tencent.mm.plugin.appbrand.ui.AppBrandLauncherUI",
//                "com.tencent.mm.plugin.appbrand.ui.AppBrandPreLoadingUI",
//                "com.tencent.mm.plugin.sns.ui.ArtistBrowseUI",
//                "com.tencent.mm.plugin.finder.feed.ui.FinderSnsGridFeedUI",
//                "com.tencent.mm.plugin.sns.ad.landingpage.ui.activity.HalfScreenVangoghPageUI",
//                "com.tencent.mm.plugin.sns.ui.SnsAdLBSAuthManagerUI",
//                "com.tencent.mm.plugin.sns.ui.SnsAdNativeLandingPagesPreviewUI",
//                "com.tencent.mm.plugin.sns.ui.SnsAdProxyUI",
//                "com.tencent.mm.plugin.sns.ui.VideoAdPlayerUI"
        };


        for (String className : adUiClasses) {
            try {
                sendLogBroadcast("Hook Log", "尝试钩住类Trying to hook class: " + className);


                // 获取并列出所有方法
                Class<?> cls = XposedHelpers.findClass(className, classLoader);
                Method[] methods = cls.getDeclaredMethods();



                for (Method method : methods) {
//                        if (method.getName().contains("$r8$lambda")) {
//                            continue;  // 跳过 Lambda 方法
//                        }
                    sendLogBroadcast("Method List", "方法Found method: " + method.getName() + ", returnType=" + method.getReturnType() + ", parameterTypes=" + Arrays.toString(method.getParameterTypes()));

                    XposedBridge.hookAllMethods(cls, method.getName(), new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            sendLogBroadcast("Event Log", "方法 " + method.getName() + " 被调用");
                        }
                    });

//                    // Get parameter types
//                    Class<?>[] parameterTypes = method.getParameterTypes();
//                    // Hook the method with specified parameter types
//                    XposedHelpers.findAndHookMethod(className, classLoader, method.getName(), parameterTypes, new XC_MethodHook() {
//                        @Override
//                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                            sendLogBroadcast("Event Log", className + " 方法 " + method.getName() + " 被调用, 参数: " + Arrays.toString(param.args));
//                        }
//
//                        @Override
//                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                            sendLogBroadcast("Event Log", className + " 方法 " + method.getName() + " 执行完毕, 返回值: " + param.getResult());
//                        }
//                    });
                }
            } catch (Throwable t) {
                sendLogBroadcast("Error Log", "钩住类失败Hook failed for class: " + className + ", error: " + t.getMessage());
            }
        }
    }

    // 调试广播发送方法
    private void sendLogBroadcast(String logTag, String logMessage) {
        try {
            XposedBridge.hookAllMethods(Application.class, "onCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    final Context context = (Context) param.thisObject;
                    // 在这里使用 context 发送广播
                    if (context != null) {
                        Intent intent = new Intent("cc.ahaly.wxskipad.LOG_BROADCAST");
                        intent.putExtra("log_tag", logTag);
                        intent.putExtra("log_message", logMessage);
                        context.sendBroadcast(intent);
                    } else {
                        XposedBridge.log("Context 为 null,无法发送调试广播。");
                    }
                }
            });
        } catch (Exception e) {
            XposedBridge.log("发送调试广播失败: " + e.getMessage());
        }
    }
}
