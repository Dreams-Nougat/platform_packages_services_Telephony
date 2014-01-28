package com.android.phone;

import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.telephony.SimInfoManager;
import android.telephony.SimInfoManager.SimInfoRecord;
import android.telephony.SubInfoRecord;

import com.android.internal.widget.SubscriptionView;

public class SubPickAdapter extends BaseAdapter{

    private static final String LOG_TAG = "SubPickAdapter";

    public static final int ITEM_TYPE_SUB      =  0;
    public static final int ITEM_TYPE_INTERNET =  1;
    public static final int ITEM_TYPE_TEXT     =  2;

    private Context mContext;
    private List<SubPickItem> mSubPickItems;
    private long mSuggestedSubId;   // used to decide whether we should show "suggest" image.
    private int mSubViewThemeType = SubscriptionView.DARK_THEME;
    // This is not dialog theme, it is used to get correct sim icon resource from framework-telephony to suit current dialog theme.

    public SubPickAdapter (Context context, int subViewThemeType, long suggestedSubId) {
        mContext = context;
        mSubViewThemeType = subViewThemeType;
        mSuggestedSubId = suggestedSubId;
    }

    public void setItems(List<SubPickItem> items) {
        mSubPickItems = items;
    }

    @Override
    public int getCount() {
        return mSubPickItems.size();
    }

    @Override
    public Object getItem(int arg0) {
        return mSubPickItems.get(arg0);
    }

    @Override
    public long getItemId(int arg0) {
        long itemId = 0;
        if (mSubPickItems.get(arg0).mViewType == ITEM_TYPE_SUB) {
            itemId = mSubPickItems.get(arg0).mSubInfoRecord.mSubId;
        }
        return itemId;
    }

    @Override
    public boolean isEnabled(int position) {
        int viewType = mSubPickItems.get(position).mViewType;
        if (viewType == ITEM_TYPE_TEXT) {
            return false;
        }
        return super.isEnabled(position);
    };

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = convertView;
        SubInfoViewHolder subInfoViewHolder = null;

        if (view == null) {
            view = inflater.inflate(R.layout.sub_pick_item, null);
            subInfoViewHolder = new SubInfoViewHolder((SubscriptionView)view.findViewById(R.id.subInfoView),
                    (ImageView)view.findViewById(R.id.suggestedView), (TextView)view.findViewById(R.id.noSubInfoText));
            view.setTag(subInfoViewHolder);
        }

        subInfoViewHolder = (SubInfoViewHolder)view.getTag();
        int viewType = mSubPickItems.get(position).mViewType;

        if (viewType == ITEM_TYPE_SUB) {
            subInfoViewHolder.mSubInfoView.setVisibility(View.VISIBLE);
            SubInfoRecord subInfoRecord = mSubPickItems.get(position).mSubInfoRecord;
            subInfoViewHolder.mSubInfoView.setThemeType(mSubViewThemeType);
            subInfoViewHolder.mSubInfoView.setSubInfo(subInfoRecord);
            if (subInfoRecord.mSubId == mSuggestedSubId) {
                subInfoViewHolder.mSuggestView.setVisibility(View.VISIBLE);
            }
            subInfoViewHolder.mNoSubInfoText.setVisibility(View.GONE);
        } else if (viewType == ITEM_TYPE_INTERNET) {
            subInfoViewHolder.mSubInfoView.setVisibility(View.VISIBLE);
            subInfoViewHolder.mSubInfoView.setSubNum(null);
            String sipName = (String)mContext.getResources().getText(R.string.incall_call_type_label_sip);
            subInfoViewHolder.mSubInfoView.setSubName(sipName);
            if (mSubViewThemeType == SubscriptionView.DARK_THEME) {
                subInfoViewHolder.mSubInfoView.setSubColor(R.drawable.sub_dark_internet_call);
            } else {
                subInfoViewHolder.mSubInfoView.setSubColor(R.drawable.sub_light_internet_call);
            }
            subInfoViewHolder.mNoSubInfoText.setVisibility(View.GONE);
        } else if (viewType == ITEM_TYPE_TEXT) {
            int simId = mSubPickItems.get(position).mSimId;
            subInfoViewHolder.mSubInfoView.setVisibility(View.INVISIBLE);
            subInfoViewHolder.mSuggestView.setVisibility(View.GONE);
            subInfoViewHolder.mNoSubInfoText.setVisibility(View.VISIBLE);
            // TODO: use string.xml to define below string
            subInfoViewHolder.mNoSubInfoText.setText("SIM Slot " + (simId + 1));
        } else {
            Log.e(LOG_TAG, "The view type of / " + viewType + " / is no a right view type !");
        }
        return view;
    }

    public static class SubInfoViewHolder {
        protected SubscriptionView mSubInfoView;
        protected ImageView mSuggestView;
        protected TextView mNoSubInfoText;

        public SubInfoViewHolder(SubscriptionView subInfoView, ImageView suggestedView, TextView noSubInfoText) {
            this.mSubInfoView = subInfoView;
            this.mSuggestView = suggestedView;
            this.mNoSubInfoText = noSubInfoText;
        }
    }

    public static class SubPickItem {
        public SubInfoRecord mSubInfoRecord;
        public int mViewType;
        public int mSimId;      // To indicate which SIM slot doesn't has inserted sim, used only when mViewType is ITEM_TYPE_TEXT

        public SubPickItem(SubInfoRecord subInfoRecord, int viewType, int simId) {
            this.mSubInfoRecord = subInfoRecord;
            this.mViewType = viewType;
            this.mSimId = simId;
        }
    }
}
