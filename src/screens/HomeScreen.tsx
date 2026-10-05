import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Dimensions,
} from 'react-native';
import AnimatedVisualizer from '../components/AnimatedVisualizer';
import BottomNavigation from '../components/BottomNavigation';

const {width, height} = Dimensions.get('window');

const HomeScreen = () => {
  return (
    <View style={styles.container}>
      {/* Main Content Area */}
      <View style={styles.mainContent}>
        {/* Main Greeting */}
        <Text style={styles.greeting}>What Can I Do for You Today?</Text>

        {/* Animated Visualizer */}
        <View style={styles.visualizerContainer}>
          <AnimatedVisualizer size={200} />
        </View>

        {/* Use Keyboard Button */}
        <TouchableOpacity style={styles.keyboardButton}>
          <View style={styles.keyboardIcon}>
            <View style={styles.iconSquare} />
            <View style={styles.iconLines}>
              <View style={styles.iconLine} />
              <View style={styles.iconLine} />
              <View style={styles.iconLine} />
            </View>
          </View>
          <Text style={styles.keyboardButtonText}>Use Keyboard</Text>
        </TouchableOpacity>
      </View>

      {/* Bottom Navigation */}
      <BottomNavigation />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000000',
  },
  mainContent: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingTop: 60,
  },
  greeting: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#FFFFFF',
    textAlign: 'center',
    marginBottom: 60,
    letterSpacing: 0.5,
  },
  visualizerContainer: {
    marginVertical: 40,
    alignItems: 'center',
    justifyContent: 'center',
  },
  keyboardButton: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 40,
    paddingVertical: 12,
    paddingHorizontal: 20,
  },
  keyboardIcon: {
    width: 20,
    height: 20,
    marginRight: 10,
    position: 'relative',
  },
  iconSquare: {
    width: 20,
    height: 20,
    borderWidth: 1.5,
    borderColor: '#FFFFFF',
    borderRadius: 2,
  },
  iconLines: {
    position: 'absolute',
    left: 4,
    top: 4,
    width: 12,
    height: 12,
    justifyContent: 'space-between',
  },
  iconLine: {
    width: 12,
    height: 1.5,
    backgroundColor: '#FFFFFF',
    marginVertical: 1.5,
  },
  keyboardButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '500',
  },
});

export default HomeScreen;
