package org.zankio.cculife.ui.transport;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.zankio.cculife.CCUService.base.listener.IOnUpdateListener;
import org.zankio.cculife.CCUService.base.source.BaseSource;
import org.zankio.cculife.CCUService.train.model.TrainTimetable;
import org.zankio.cculife.R;
import org.zankio.cculife.ui.base.BaseMessageFragment;


public class TrainFragment extends BaseMessageFragment implements ISwitchLine {
    private static final String KEY_TRAIN = "TRAIN";

    private IGetTrainData context;
    private TrainStopAdapter adapter;
    private String trainStop;
    private TrainTimetable timetable;
    private int currentLine = 0;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.context = (IGetTrainData) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement IGetTrainData");
        }
    }

    public static Fragment getInstance(String trainStop) {
        TrainFragment fragment = new TrainFragment();
        fragment.setArguments(getArgument(trainStop));
        return fragment;
    }

    public static Bundle getArgument(String trainStop) {
        Bundle arguments = new Bundle();
        arguments.putString(KEY_TRAIN, trainStop);
        return arguments;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_train, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle arguments = getArguments();
        trainStop = arguments.getString(KEY_TRAIN);

        View footer = View.inflate(getContext(), R.layout.transport_list_footer, null);
        ((TextView) footer.findViewById(R.id.footer_text)).setText("資料來源：\n臺灣鐵路管理局\n列車動態查詢系統");

        ListView listView = (ListView) view.findViewById(R.id.list);
        listView.addFooterView(footer, null, false);

        adapter = new TrainStopAdapter();
        updateTrain();
    }

    public void updateTrain() {
        View view = getView();
        if (view == null) return;


        ((TextView) view.findViewById(R.id.train_bound)).setText(currentLine == 0 ? "上行" : "下行");
        ListView listView = (ListView) view.findViewById(R.id.list);
        listView.setAdapter(adapter);

        if (timetable == null) {
            message().show("讀取中...", true);
            context.getTrainStatus(trainStop, listener);
        } else {
            adapter.setTimetable(currentLine == 0 ? timetable.up : timetable.down);
            adapter.notifyDataSetChanged();
        }
    }

    public IOnUpdateListener<TrainTimetable> listener = new IOnUpdateListener<TrainTimetable>() {
        @Override
        public void onNext(String type, TrainTimetable timetable, BaseSource source) {
            TrainFragment.this.timetable = timetable;
            adapter.setTimetable(currentLine == 0 ? timetable.up : timetable.down);
            adapter.notifyDataSetChanged();
            message().hide();
        }

        @Override
        public void onError(String type, Exception err, BaseSource source) {
            message().show(err.getMessage());
        }

        @Override
        public void onComplete(String type) { }
    };

    @Override
    public void swtichLine() {
        currentLine = (currentLine + 1) % 2;
        updateTrain();
    }

    private class TrainStopAdapter extends BaseAdapter {
        private TrainTimetable.Item[] timetable;

        public void setTimetable(TrainTimetable.Item[] timetable) {
            this.timetable = timetable;
        }

        @Override
        public int getCount() {
            return timetable == null ? 0 : timetable.length;
        }

        @Override
        public Object getItem(int position) {
            return timetable == null ? null : timetable[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = LayoutInflater
                        .from(getContext())
                        .inflate(R.layout.item_train, parent, false);

            TrainTimetable.Item trainStop = timetable[position];

            ((TextView) convertView.findViewById(R.id.train_delay)).setText(trainStop.delay);
            ((TextView) convertView.findViewById(R.id.train_code)).setText(trainStop.code);
            ((TextView) convertView.findViewById(R.id.train_to)).setText(trainStop.to);
            ((TextView) convertView.findViewById(R.id.train_departure)).setText(trainStop.departure);
            ((TextView) convertView.findViewById(R.id.train_type)).setText(trainStop.type);
            return convertView;
        }
    }
}