package lb.edu.aust.ict499.schoolsecured;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.myViewHolder> {

    private final HashMap<String, String> studentData;
    private final onStudentNameClickListener listener2;

    public RecyclerAdapter(HashMap<String, String> students, onStudentNameClickListener l){
        this.studentData = students;
        this.listener2 = l;
    }

    public static class myViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView studentName;
        private final TextView moduleID;
        onStudentNameClickListener listener;

        public myViewHolder (final View view, onStudentNameClickListener listener1){
            super(view);
            studentName = view.findViewById(R.id.tv_studentName);
            moduleID = view.findViewById(R.id.tv_ModuleID);
            this.listener = listener1;
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            this.listener.onClick(getAdapterPosition());
        }
    }

    @NonNull
    @Override
    public RecyclerAdapter.myViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_list_items, parent, false);
        return new myViewHolder(itemView, listener2);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerAdapter.myViewHolder holder, int position) {
        Set<String> keyset = this.studentData.keySet();
        ArrayList<String> listofkeys = new ArrayList<String>(keyset);
        Collection<String> values = this.studentData.values();
        ArrayList<String> listofvalues = new ArrayList<String>(values);

        holder.studentName.setText(listofkeys.get(position));
        holder.moduleID.setText(listofvalues.get(position));
    }

    @Override
    public int getItemCount() {
        return studentData.size();
    }

    public interface onStudentNameClickListener {
        void onClick(int position);
    }
}
