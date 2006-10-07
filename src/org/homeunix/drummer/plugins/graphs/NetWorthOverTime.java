/*
 * Created on Sep 14, 2006 by wyatt
 */
package org.homeunix.drummer.plugins.graphs;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.JPanel;

import org.homeunix.drummer.controller.Translate;
import org.homeunix.drummer.controller.TranslateKeys;
import org.homeunix.drummer.model.Account;
import org.homeunix.drummer.model.DataInstance;
import org.homeunix.drummer.model.Transaction;
import org.homeunix.drummer.plugins.BuddiGraphPlugin;
import org.homeunix.drummer.plugins.BuddiPluginFactory.DateRangeType;
import org.homeunix.thecave.moss.util.DateUtil;
import org.homeunix.thecave.moss.util.Formatter;
import org.homeunix.thecave.moss.util.Log;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

public class NetWorthOverTime implements BuddiGraphPlugin {
	public DateRangeType getDateRangeType() {
		return DateRangeType.START_ONLY;
	}
	public String getDescription() {
		return TranslateKeys.NETWORTH_LINE_GRAPH.toString();
	}
	public JPanel getGraphPanel(Date startDate, Date endDate) {
		final int NUM_SAMPLES = 12;
		
		DefaultCategoryDataset barData = new DefaultCategoryDataset();
		
		Vector<Date> dates = new Vector<Date>();
		Date date = startDate;
		
		int numberOfDaysBetween = DateUtil.daysBetween(startDate, new Date());
		int daysBetweenReport = numberOfDaysBetween / NUM_SAMPLES;
		
		for (int i = 0; i < NUM_SAMPLES; i++){
			date = DateUtil.getNextNDay(startDate, i * daysBetweenReport);
			if (date.before(new Date()))
				dates.add(date);
			Log.debug("Added date: " + date);			
		}
		
		dates.add(new Date());
		
		for (Date d : dates) {
			Map<Account, Long> accounts = getAccountBalance(d);
			
			long total = 0;
			for (Account a : accounts.keySet()) {
//				barData.addValue((Number) new Double(accounts.get(a) / 100.0), Formatter.getInstance().getDateFormat().format(d), a.getName());
				total += accounts.get(a);
			}
			barData.addValue((Number) new Double(total / 100.0), Translate.getInstance().get(TranslateKeys.NET_WORTH), Formatter.getInstance().getShortDateFormat().format(d));
		}
		
		
		
		JFreeChart chart = ChartFactory.createLineChart(
				Translate.getInstance().get(TranslateKeys.NET_WORTH)				
				+ " (" 
				+ Formatter.getInstance().getDateFormat().format(startDate)
				+ " - "
				+ Formatter.getInstance().getDateFormat().format(new Date())
				+ ")",
				"", //Domain axis label
				"", //Range axis label
				barData,             // data
				PlotOrientation.VERTICAL,
				true,               // include legend
				true,
				false
		);
		
		CategoryPlot plot = (CategoryPlot) chart.getCategoryPlot();
		plot.setNoDataMessage("No data available");
		
		return new ChartPanel(chart);
	}
	
	public String getTitle() {
		return "";
	}
	
	private Map<Account, Long> getAccountBalance(Date date){
		Map<Account, Long> map = new HashMap<Account, Long>();
		
		for (Account a : DataInstance.getInstance().getAccounts()) {
			if (a.getCreationDate().before(date))
				map.put(a, a.getStartingBalance());
			else
				map.put(a, 0l);
		}
		
		Vector<Transaction> transactions = DataInstance.getInstance().getTransactions();
		
		for (Transaction transaction : transactions) {
			if (transaction.getDate().before(date)){
				//We are moving money *to* this account
				if (transaction.getTo() instanceof Account){
					Account a = (Account) transaction.getTo();
					map.put(a, map.get(a) + transaction.getAmount());
				}
				
				//We are moving money *from* this account
				if (transaction.getFrom() instanceof Account){
					Account a = (Account) transaction.getFrom();
					map.put(a, map.get(a) - transaction.getAmount());
				}
			}
			else{
				Log.debug("Not including transaction.");
			}
		}
		
		return map;
	}
}
