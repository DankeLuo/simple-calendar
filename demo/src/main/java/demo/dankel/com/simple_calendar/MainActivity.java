package demo.dankel.com.simple_calendar;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.Date;

import demo.dankel.com.calendar_lib.MonthView;

public class MainActivity extends AppCompatActivity {

    private boolean flag = true;
    private MonthView mMonthView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMonthView = (MonthView) findViewById(R.id.monthView);
        mMonthView.setOnSelectedDateChangedListener(new MonthView.OnSelectedDateChangedListener() {
            @Override
            public void onChanged(Date mSelectedDate) {
                Toast.makeText(MainActivity.this, "选择了" + mSelectedDate.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_show_lunar:
                Toast.makeText(MainActivity.this, "即将推出", Toast.LENGTH_SHORT).show();
                break;
            case R.id.menu_back_to_today:
                mMonthView.backToToday();
                break;
            case R.id.menu_first_day_of_week:
                flag = !flag;
                mMonthView.setSundayFirstDayOfWeek(flag);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
