/*
 *  Copyright (C) 2011 John Törnblom
 *
 * This file is part of TVHGuide.
 *
 * TVHGuide is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TVHGuide is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TVHGuide.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.me.tvhguide;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.me.tvhguide.htsp.HTSListener;
import org.me.tvhguide.htsp.HTSService;
import org.me.tvhguide.model.Channel;
import org.me.tvhguide.model.Programme;

/**
 *
 * @author john-tornblom
 */
public class ProgrammeListActivity extends ListActivity implements HTSListener {

    private ProgrammeListAdapter prAdapter;
    private Channel channel;
    private String[] contentTypes;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        channel = app.getChannel(getIntent().getLongExtra("channelId", 0));
        if (channel == null || channel.epg.isEmpty()) {
            finish();
            return;
        }

        contentTypes = getResources().getStringArray(R.array.pr_type);

        setTitle(channel.name);

        Button btn = new Button(this);
        btn.setText(R.string.pr_get_more);
        btn.setOnClickListener(new OnClickListener() {

            public void onClick(View view) {
                Programme p = prAdapter.getItem(prAdapter.getCount() - 1);

                Intent intent = new Intent(ProgrammeListActivity.this, HTSService.class);
                intent.setAction(HTSService.ACTION_GET_EVENTS);
                intent.putExtra("eventId", p.id);
                intent.putExtra("channelId", channel.id);
                intent.putExtra("count", 10);
                startService(intent);
            }
        });
        getListView().addFooterView(btn);

        List<Programme> prList = new ArrayList<Programme>();
        prList.addAll(channel.epg);
        prAdapter = new ProgrammeListAdapter(this, prList);
        setListAdapter(prAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        app.addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        TVHGuideApplication app = (TVHGuideApplication) getApplication();
        app.removeListener(this);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Programme p = (Programme) prAdapter.getItem(position);

        Intent intent = new Intent(this, ProgrammeActivity.class);
        intent.putExtra("eventId", p.id);
        intent.putExtra("channelId", channel.id);
        startActivity(intent);
    }

    public void onMessage(String action, final Object obj) {
        if (action.equals(TVHGuideApplication.ACTION_PROGRAMME_ADD)) {
            runOnUiThread(new Runnable() {

                public void run() {
                    Programme p = (Programme) obj;
                    if (p.channel.id == channel.id) {
                        prAdapter.add(p);
                        prAdapter.notifyDataSetChanged();
                        prAdapter.sort();
                    }
                }
            });
        } else if (action.equals(TVHGuideApplication.ACTION_PROGRAMME_DELETE)) {
            runOnUiThread(new Runnable() {

                public void run() {
                    Programme p = (Programme) obj;
                    if (p.channel.id == channel.id) {
                        prAdapter.remove(p);
                        prAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    private class ViewWarpper {

        TextView title;
        TextView channel;
        TextView time;
        TextView date;
        TextView description;
        TextView type;
        ImageView icon;

        public ViewWarpper(View base) {
            title = (TextView) base.findViewById(R.id.pr_title);
            type = (TextView) base.findViewById(R.id.pr_type);
            description = (TextView) base.findViewById(R.id.pr_desc);

            time = (TextView) base.findViewById(R.id.pr_time);
            date = (TextView) base.findViewById(R.id.pr_date);

            icon = (ImageView) base.findViewById(R.id.pr_icon);
            icon.setVisibility(ImageView.GONE);
        }

        public void repaint(Programme p) {
            title.setText(p.title);
            title.invalidate();

            date.setText(DateFormat.getMediumDateFormat(date.getContext()).format(p.start));
            date.invalidate();

            description.setText(p.description);
            description.invalidate();

            if(p.type > 0 && p.type < 11) {
                type.setText(contentTypes[p.type-1]);
            } else {
                type.setText("");
            }
            type.invalidate();
            
            date.setText(DateFormat.getMediumDateFormat(date.getContext()).format(p.start));
            date.invalidate();

            time.setText(
                    DateFormat.getTimeFormat(time.getContext()).format(p.start)
                    + " - "
                    + DateFormat.getTimeFormat(time.getContext()).format(p.stop));
            time.invalidate();
        }
    }

    class ProgrammeListAdapter extends ArrayAdapter<Programme> {

        Activity context;
        List<Programme> list;

        ProgrammeListAdapter(Activity context, List<Programme> list) {
            super(context, R.layout.pr_widget, list);
            this.context = context;
            this.list = list;
        }

        public void sort() {
            sort(new Comparator<Programme>() {

                public int compare(Programme x, Programme y) {
                    return x.compareTo(y);
                }
            });
        }

        public void updateView(ListView listView, Programme programme) {
            for (int i = 0; i < listView.getChildCount(); i++) {
                View view = listView.getChildAt(i);
                int pos = listView.getPositionForView(view);
                Programme pr = (Programme) listView.getItemAtPosition(pos);

                if (view.getTag() == null || pr == null) {
                    continue;
                }

                if (programme.id != pr.id) {
                    continue;
                }

                ViewWarpper wrapper = (ViewWarpper) view.getTag();
                wrapper.repaint(programme);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            ViewWarpper wrapper = null;

            Programme p = list.get(position);

            if (row == null) {
                LayoutInflater inflater = context.getLayoutInflater();
                row = inflater.inflate(R.layout.pr_widget, null, false);

                wrapper = new ViewWarpper(row);
                row.setTag(wrapper);

            } else {
                wrapper = (ViewWarpper) row.getTag();
            }

            wrapper.repaint(p);
            return row;
        }
    }
}
