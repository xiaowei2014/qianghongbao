package my.learn.qianghongbao;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.*;

/**
 *
 * @author 张小伟
 *
 */
public class QiangHongBaoService extends AccessibilityService{
	static final String TAG = "QiangHongBao";

	/** 微信的包名*/
	static final String WECHAT_PACKAGENAME = "com.tencent.mm";
	/** 红包消息的关键字*/
	static final String HONGBAO_TEXT_KEY = "[微信红包]";




	Set<String> sourceNodeSet = new HashSet<>();

	Handler handler = new Handler();

	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		final int eventType = event.getEventType();

//		Log.d(TAG, "事件---->"+event.getRecordCount()+":" + event);
//	        System.out.println("事件---->"+event.getRecordCount()+":" + event);
//		Log.d(TAG,"事件--->" + AccessibilityEvent.eventTypeToString(eventType) + "," + event.getClassName() + "," + event.getContentDescription());
		//通知栏事件
		if(eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
			List<CharSequence> texts = event.getText();
			if(!texts.isEmpty()) {
				for(CharSequence t : texts) {
					String text = String.valueOf(t);
					if(text.contains(HONGBAO_TEXT_KEY)) {
						openNotify(event);
						break;
					}
				}
			}
		} else if(eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
			windowChanged(event);
		}else if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ){
			windowChanged(event);
		}
	}

	    /*@Override
	    protected boolean onKeyEvent(KeyEvent event) {
	        //return super.onKeyEvent(event);
	        return true;
	    }*/

	@Override
	public void onInterrupt() {
		Toast.makeText(this, "中断抢红包服务", Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onServiceConnected() {
		super.onServiceConnected();
		Toast.makeText(this, "连接抢红包服务", Toast.LENGTH_SHORT).show();
	}

	private void sendNotifyEvent(){
		AccessibilityManager manager= (AccessibilityManager)getSystemService(ACCESSIBILITY_SERVICE);
		if (!manager.isEnabled()) {
			return;
		}
		AccessibilityEvent event=AccessibilityEvent.obtain(AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED);
		event.setPackageName(WECHAT_PACKAGENAME);
		event.setClassName(Notification.class.getName());
		CharSequence tickerText = HONGBAO_TEXT_KEY;
		event.getText().add(tickerText);
		manager.sendAccessibilityEvent(event);
	}

	/** 打开通知栏消息*/
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void openNotify(AccessibilityEvent event) {
		if(event.getParcelableData() == null || !(event.getParcelableData() instanceof Notification)) {
			return;
		}
		//以下是精华，将微信的通知栏消息打开
		Notification notification = (Notification) event.getParcelableData();
		PendingIntent pendingIntent = notification.contentIntent;
		try {
			pendingIntent.send();
		} catch (PendingIntent.CanceledException e) {
			e.printStackTrace();
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void windowChanged(AccessibilityEvent event) {
//		if("com.tencent.mm.ui.LauncherUI".equals(event.getClassName())) {
		if("android.widget.ListView".equals(event.getClassName())) {
			//在聊天界面,去点中红包
			lingquhongbao();
		}else if("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI".equals(event.getClassName())) {
			//点中了红包，下一步就是去拆红包
			chaihongbao();
		} else if("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI".equals(event.getClassName())) {
			//拆完红包后看详细的纪录界面
			backChatWindow();
		}
	}

	private void backChatWindow() {
		AccessibilityNodeInfo root =	getRootNode();
		List<AccessibilityNodeInfo> list=root.findAccessibilityNodeInfosByText("红包详情");
		if (list == null || list.size() ==0) return;
		for (AccessibilityNodeInfo node : list){
			AccessibilityNodeInfo parent = node.getParent();
			parent.getChild(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);

		}

	}

	private AccessibilityNodeInfo getRootNode(){
		AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
		if(nodeInfo == null) {
			Log.w(TAG, "rootWindow为空");
			return null;
		}
		return nodeInfo;
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void chaihongbao() {
		AccessibilityNodeInfo nodeInfo =getRootNode();
		if(nodeInfo == null) {
			return;
		}
		List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("拆红包");
		for(AccessibilityNodeInfo n : list) {
			n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
		}
		if (! list.isEmpty()){
			return;
		}
		List<AccessibilityNodeInfo> shouqi = nodeInfo.findAccessibilityNodeInfosByText("看看大家的手气");
		if (shouqi.size() >0){
			shouqi.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
		}

	}

	Method sourceIdMethod;

	private Method getSourceIdMethod(){
		if (sourceIdMethod == null){
			try {
				 sourceIdMethod = AccessibilityNodeInfo.class.getMethod("getSourceNodeId");
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			}
		}
		return sourceIdMethod;
	}

	private long getSourceId(AccessibilityNodeInfo node){

		try {
			getSourceIdMethod().setAccessible(true);
			return  (long) sourceIdMethod.invoke(node);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return  0;
	}

	private Method parentIdMethod = null;
	private Method getParentIdMethod(){
		if (parentIdMethod == null){
			try {
				parentIdMethod = AccessibilityNodeInfo.class.getMethod("getParentNodeId");
				parentIdMethod.setAccessible(true);
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			}
		}
		return parentIdMethod;
	}
	private long getParentId(AccessibilityNodeInfo node){
		try {
			return  (long) getParentIdMethod().invoke(node);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return  0;
	}


	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void lingquhongbao() {
		AccessibilityNodeInfo nodeInfo = getRootNode();
		if(nodeInfo == null) {
			return;
		}

		List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("领取红包");
		if(list!=null && !list.isEmpty()) {
			for (AccessibilityNodeInfo node :list){
				String key = getNodeHashCode(node);
				if (sourceNodeSet.contains(key)){
					continue;
				}
				node.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
				sourceNodeSet.add(key);
			}
		}

	}

	private String getNodeHashCode(AccessibilityNodeInfo node) {
		StringBuffer sb = new StringBuffer();
		long sourceId = getSourceId(node);
		long parentId = getParentId(node);

		sb.append(parentId).append("_").append(sourceId);

		AccessibilityNodeInfo parent = node.getParent();
		int count = parent.getChildCount();
		for (int i=0;i<count ;i++){
			sb.append("_").append(parent.getChild(i).getText());
		}

//		Log.d(TAG,sb.toString());
		return sb.toString();
	}
}
