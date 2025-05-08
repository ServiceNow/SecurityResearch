import argparse
import matplotlib.pyplot as plt
import numpy as np

# function to simulate GAN training losses with AI-generated malware complexity
def simulate_gan_losses(num_epochs, gan_type, team, malware_type):
    """
    simulate generator and discriminator losses over epochs, adjusted for AI-generated malware.
    
    Args:
        num_epochs (int): Number of training epochs.
        gan_type (str): Type of GAN ('wgan-gp' or 'standard').
        team (str): 'red' for evasion, 'blue' for detection.
        malware_type (str): 'ransomware', 'trojan', or 'other'.
    
    Returns:
        tuple: (epochs, discriminator_loss, generator_loss)
    """
    # simulate epochs in steps for smoother curves
    epoch_step = max(1, num_epochs // 100)  # Adjust step size based on epochs
    epochs = np.arange(0, num_epochs + 1, epoch_step)

    # define base decay rates and noise levels based on malware type and team
    if malware_type == "ransomware":
        base_decay = 0.0006 if team == "red" else 0.0004
        noise_scale = 0.005 if team == "red" else 0.003  # Higher noise for evasion
    elif malware_type == "trojan":
        base_decay = 0.0005 if team == "red" else 0.0003
        noise_scale = 0.004  # Stealth requires moderate noise
    else:  # Other malware
        base_decay = 0.0005
        noise_scale = 0.002

    # set initial and final loss values based on GAN type
    if gan_type == "wgan-gp":
        d_loss_start, d_loss_end = -0.0213, -0.0030  # Negative for WGAN-GP
        g_loss_start, g_loss_end = 0.0157, 0.0020
    else:  # Standard GAN
        d_loss_start, d_loss_end = 0.693, 0.5  # Log(2) as initial loss
        g_loss_start, g_loss_end = 0.693, 0.5

    # calculate base exponential decay
    d_loss = d_loss_start * np.exp(-base_decay * epochs) + d_loss_end
    g_loss = g_loss_start * np.exp(-base_decay * epochs) + g_loss_end

    # add Gaussian noise to simulate AI-generated malware complexity
    np.random.seed(42)  # For reproducibility
    d_noise = np.random.normal(0, noise_scale, size=epochs.shape)
    g_noise = np.random.normal(0, noise_scale, size=epochs.shape)
    d_loss = np.clip(d_loss + d_noise, np.min(d_loss) - 0.01, np.max(d_loss) + 0.01)
    g_loss = np.clip(g_loss + g_noise, np.min(g_loss) - 0.01, np.max(g_loss) + 0.01)

    return epochs, d_loss, g_loss

# function to plot the convergence graph
def plot_convergence(epochs, d_loss, g_loss, gan_type, team, malware_type):
    """
    Plot GAN training convergence with dual Y-axes for discriminator and generator losses.
    
    Args:
        epochs (np.array): Array of epoch numbers.
        d_loss (np.array): Discriminator loss values.
        g_loss (np.array): Generator loss values.
        gan_type (str): Type of GAN.
        team (str): Team objective.
        malware_type (str): Type of malware.
    """
    fig, ax1 = plt.subplots(figsize=(12, 7))

    # left Y-axis: discriminator loss
    ax1.set_xlabel('Epochs')
    ax1.set_ylabel('Discriminator Loss', color='tab:blue')
    ax1.plot(epochs, d_loss, color='tab:blue', label='Discriminator Loss', alpha=0.7)
    ax1.tick_params(axis='y', labelcolor='tab:blue')
    ax1.set_ylim(np.min(d_loss) - 0.02, np.max(d_loss) + 0.02)
    ax1.grid(True, linestyle='--', alpha=0.5)

    # right Y-axis: generator loss
    ax2 = ax1.twinx()
    ax2.set_ylabel('Generator Loss', color='tab:red')
    ax2.plot(epochs, g_loss, color='tab:red', label='Generator Loss', alpha=0.7)
    ax2.tick_params(axis='y', labelcolor='tab:red')
    ax2.set_ylim(np.min(g_loss) - 0.02, np.max(g_loss) + 0.02)

    # title and layout
    team_str = "Red Team (Evasion)" if team == "red" else "Blue Team (Detection)"
    plt.title(f'GAN Training Convergence\n'
              f'({gan_type.upper()} - {malware_type.capitalize()} - {team_str})',
              fontsize=14, pad=20)
    fig.tight_layout()

    # adds legend
    lines1, labels1 = ax1.get_legend_handles_labels()
    lines2, labels2 = ax2.get_legend_handles_labels()
    ax1.legend(lines1 + lines2, labels1 + labels2, loc='upper right')

    plt.show()

# main function with command-line argument handling
def main():
    parser = argparse.ArgumentParser(description="GAN Training Convergence Graph Generator for AI-Generated Malware")
    parser.add_argument('--epochs', type=int, default=10000, help='Number of training epochs (default: 10000)')
    parser.add_argument('--lr', type=float, default=0.0001, help='Learning rate (default: 0.0001)')
    parser.add_argument('--batch_size', type=int, default=32, help='Batch size (default: 32)')
    parser.add_argument('--gan_type', type=str, choices=['wgan-gp', 'standard'], default='wgan-gp',
                        help='Type of GAN (default: wgan-gp)')
    parser.add_argument('--team', type=str, choices=['red', 'blue'], default='red',
                        help='Team objective: red for evasion, blue for detection (default: red)')
    parser.add_argument('--malware_type', type=str, choices=['ransomware', 'trojan', 'other'], default='other',
                        help='Type of malware (default: other)')

    args = parser.parse_args()

    # adjusts params based on team and malware type
    if args.team == "red":
        # Red team: Increase epochs for evasion tasks
        if args.epochs == 10000:  # Only adjust default
            args.epochs = 12000 if args.malware_type == "ransomware" else 10000
    elif args.team == "blue":
        # Blue team: Use standard GAN for stability
        if args.gan_type == "wgan-gp" and args.malware_type != "ransomware":
            args.gan_type = "standard"

    # prints the simulation details
    print(f"Simulating GAN training for {args.malware_type.capitalize()}:\n"
          f"- Epochs: {args.epochs}\n"
          f"- Learning Rate: {args.lr}\n"
          f"- Batch Size: {args.batch_size}\n"
          f"- GAN Type: {args.gan_type.upper()}\n"
          f"- Team: {'Red (Evasion)' if args.team == 'red' else 'Blue (Detection)'}")

    # simulates losses
    epochs, d_loss, g_loss = simulate_gan_losses(args.epochs, args.gan_type, args.team, args.malware_type)

    # plots the convergence graph
    plot_convergence(epochs, d_loss, g_loss, args.gan_type, args.team, args.malware_type)

if __name__ == "__main__":
    main()
